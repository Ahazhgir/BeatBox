import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.*;

public class MiniMiniMusicApp {
	JFrame theFrame;
	JPanel mainPanel;
	JList incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkboxList; //тут хранятся флажки
	int nextNum;
	Vector<String> listVector = new Vector<>();
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
	
	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;
	
	//названия инструментов
	String[] instrumentNames = {"Большой Барабан", "Closed Hi-Hat", "Open Hi-Hat", "Акустический малый Барабан", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Вистл", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
	//барабанные клавиши
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

	public static void main(String[] args) {
		
		new MiniMiniMusicApp().startUp("Test");
	}
	
	public void startUp(String name) {
		userName = name;
		//открываем соединение с сервером
		try {
			Socket sock = new Socket("127.0.0.1", 4243);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader());
			remote.start();
		} catch(Exception e) {
			System.out.println("не удалось подключиться - придется играть одному");
		}
		setUpMidi();
		buildGUI();
	}
	
	public void buildGUI() {
		theFrame = new JFrame("Кибер БитБокс");
		BorderLayout layout = new BorderLayout();
		JPanel backgraund = new JPanel(layout);
		backgraund.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		checkboxList = new ArrayList<JCheckBox>();
		
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		JButton start = new JButton("Старт");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);
		
		JButton stop = new JButton("Стоп");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);
		
		JButton upTempo = new JButton("Повысить темп");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);
		
		JButton downTempo = new JButton("Понизить темп");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);
		
		JButton save = new JButton("Отправить");
		save.addActionListener(new MySendListener());
		buttonBox.add(save);
		
		JButton save1 = new JButton("Сохранить");
		save1.addActionListener(new MySendListener1());
		buttonBox.add(save1);
		
		JButton read = new JButton("Восстановить");
		read.addActionListener(new MyReadInListener());
		buttonBox.add(read);
		
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		userMessage = new JTextField();
		buttonBox.add(userMessage);
		
		incomingList = new JList();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector); //нет начальных данных
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i = 0; i < 16; i++) {
			nameBox.add(new Label(instrumentNames[i]));
		}
		
		backgraund.add(BorderLayout.EAST, buttonBox);
		backgraund.add(BorderLayout.WEST, nameBox);
		
		theFrame.getContentPane().add(backgraund);
		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		backgraund.add(BorderLayout.CENTER, mainPanel);
		
		for(int i = 0; i < 256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
			//создаем флажки, присваиваем им значение ложь и добавляем в список и на панель
		}
		
		theFrame.setBounds(50, 50, 300, 300);
		theFrame.pack();
		theFrame.setVisible(true);
	}
	
	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void buildTrackAndStart() {
		ArrayList<Integer> trackList = null;
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i = 0; i < 16; i++) {
			trackList = new ArrayList<Integer>();
						
			for(int j = 0; j < 16; j++) {
				JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i));
				if(jc.isSelected()) {
					int key = instruments[i];
					trackList.add(key);
				} else {
					trackList.add(0); //этот слот в треке должен быть пустым
				}
			}
			
			makeTracks(trackList);
//			track.add(makeEvent(176, 1, 127, 0, 16));
		}
		
		track.add(makeEvent(192, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public class MyStartListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			buildTrackAndStart();
			
		}
	}
	
	public class MyStopListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			sequencer.stop();
			
		}
	}
	
	public class MyUpTempoListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 1.03));
			
		}
		
	}
	
	public class MyDownTempoListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * .97));
			
		}
		
	}
	
	public class MySendListener1 implements ActionListener {
		boolean[] checkboxState = new boolean[256];

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			
			for(int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			
			JFileChooser fileSave = new JFileChooser();
			fileSave.showSaveDialog(theFrame);
			saveFile(fileSave.getSelectedFile());
			
			
			
		}
		
		private void saveFile(File file) {
			try {
				FileOutputStream fileStream = new FileOutputStream(file);
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkboxState);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public class MySendListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			boolean[] checkboxState = new boolean[256];
			
			for(int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			
			String messageToSend = null;
			try {
				out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
				out.writeObject(checkboxState);
			} catch(Exception e ) {
				System.out.println("Извини чувак. Не удалось отправить на сервер");
			}
			userMessage.setText("");
			
		}
		
	}
	
	public class MyListSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			if(!arg0.getValueIsAdjusting()) {
				String selected = (String) incomingList.getSelectedValue();
				if(selected != null) {
					//переходим к отображению и изменяем последовательность
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
			
		}
		
	}
	
	public class RemoteReader implements Runnable {
		boolean[] checkboxState = null;
		String nameToShow = null;
		Object obj = null;

		@Override
		public void run() {
			try {
				while((obj = in.readObject()) != null) {
					System.out.println("получили объект с сервера");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					checkboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkboxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	public class MyPlayMineListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(mySequence != null) {
				sequence = mySequence;
			}
			
		}
		
	}
	
	public void changeSequence(boolean[] checkboxState) {
		for(int i = 0; i < 256; i++) {
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if(checkboxState[i]) {
				check.setSelected(true);
			} else {
				check.setSelected(false);
			}
		}
	}
	
	public void makeTracks(ArrayList list) {
		Iterator it = list.iterator();
		for(int i =0; i < 16; i++) {
			Integer num = (Integer) it.next();			
			if(num != 0) {
				int numKey = num.intValue();
				track.add(makeEvent(144, 9, numKey, 100, i));
				track.add(makeEvent(128, 9, numKey, 100, i+1));
				
			}
		}
	}
	
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, tick);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return event;
	}
	
	
	
	public class MyReadInListener implements ActionListener {
		boolean[] checkboxState = null;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			
			JFileChooser fileOpen = new JFileChooser();
			fileOpen.showOpenDialog(theFrame);
			loadFile(fileOpen.getSelectedFile());
			
			for(int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(checkboxState[i]) {
					check.setSelected(true);
				} else {
					check.setSelected(false);
				}
			}
			
			sequencer.stop();
			buildTrackAndStart();
			
		}
		
		private void loadFile(File file) {
			try {
				FileInputStream fileIn = new FileInputStream(file);
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkboxState = (boolean[]) is.readObject();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
}
