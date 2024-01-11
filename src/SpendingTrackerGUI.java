import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class SpendingTrackerGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final String SETTINGS_FILE_PATH = "settings.dat";
	private static final String SPENDINGS_FILE_PATH = "spendings.dat";

	@SuppressWarnings("unused")
	private String username;
	@SuppressWarnings("unused")
	private String password;
	private JTable table;
	private DefaultTableModel tableModel;
	private boolean loggedIn = false;
	private boolean loginCancelled = false;
	private JLabel totalAmountLabel;

	public SpendingTrackerGUI() {
		setTitle("Spending Tracker");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				saveSpendings(); // Call saveSpendings() when the window is closing
				quit();
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		createMenuBar();
		createTable();
		createTotalAmountLabel();
		loadSettings();
		authenticate();
		loadSpendings();
		scheduleNotification();
		loadSettings();
	}

	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenuItem addSpendingsItem = new JMenuItem("Add Spendings");
		addSpendingsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addSpendings();
			}
		});
		fileMenu.add(addSpendingsItem);

		JMenuItem editSpendingsItem = new JMenuItem("Edit Spendings");
		editSpendingsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editSpendings();
			}
		});
		fileMenu.add(editSpendingsItem);

		JMenuItem deleteSpendingsItem = new JMenuItem("Delete Spendings");
		deleteSpendingsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteSpending();
			}
		});
		fileMenu.add(deleteSpendingsItem);

		JMenuItem sortSpendingsItem = new JMenuItem("Sort Spendings");
		sortSpendingsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showSortOptions();
			}
		});
		fileMenu.add(sortSpendingsItem);

		
	}

	private void showSortOptions() {
		Object[] options = { "Alphabetical (Descending)", "Alphabetical (Ascending)", "Lowest Amount to Highest",
				"Highest Amount to Lowest", "Newest to Oldest", "Oldest to Newest" };

		int sortOption = JOptionPane.showOptionDialog(this, "Select sorting option:", "Sort Spendings",
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

		sortSpendings(sortOption + 1);
	}

	private void deleteSpending() {
		int selectedRow = table.getSelectedRow();

		if (selectedRow >= 0) {
			tableModel.removeRow(selectedRow);
			saveSpendings();
		} else {
			JOptionPane.showMessageDialog(this, "No spending record selected.", "No Selection",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void createTable() {
		tableModel = new DefaultTableModel(new Object[] { "Date", "Subject", "Amount (Rs.)" }, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

		};

		table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		tableModel.addTableModelListener(e -> updateTotalAmount());
	}

	private void createTotalAmountLabel() {
		totalAmountLabel = new JLabel("Total Amount: Rs. 0.00");
		getContentPane().add(totalAmountLabel, BorderLayout.SOUTH);
	}

	private void updateTotalAmount() {
		double totalAmount = 0;

		for (int row = 0; row < tableModel.getRowCount(); row++) {
			double amount = (double) tableModel.getValueAt(row, 2);
			totalAmount += amount;
		}

		totalAmountLabel.setText("Total Amount: Rs. " + String.format("%.2f", totalAmount));
	}

	private boolean authenticate() {
		String savedUsername = null;
		String savedPassword = null;

		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SETTINGS_FILE_PATH))) {
			savedUsername = (String) in.readObject();
			savedPassword = (String) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			createAccount();
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SETTINGS_FILE_PATH))) {
				savedUsername = (String) in.readObject();
				savedPassword = (String) in.readObject();
			} catch (IOException | ClassNotFoundException ex) {
				JOptionPane.showMessageDialog(this, "Error loading account credentials.", "Authentication Failed",
						JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
		}

		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SETTINGS_FILE_PATH))) {
			savedUsername = (String) in.readObject();
			savedPassword = (String) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			createAccount();
		}

		if (savedUsername != null && savedPassword != null) {
			String enteredPassword = getPassword(savedUsername);

			if (enteredPassword == null) {
				loginCancelled = true;
				return false;
			}

			if (savedPassword.equals(enteredPassword)) {
				this.username = savedUsername;
				this.password = savedPassword;
				return true;
			} else {
				JOptionPane.showMessageDialog(this, "Invalid password.", "Authentication Failed",
						JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
		} else {
			JOptionPane.showMessageDialog(this, "Invalid username or password. Please try again.", "Error",
					JOptionPane.ERROR_MESSAGE);
		}

		return false;
	}

	private void sortSpendings(int sortOption) {
		switch (sortOption) {
		case 1:
			tableModel.getDataVector().sort((row1, row2) -> {
				String subject1 = (String) row1.get(1);
				String subject2 = (String) row2.get(1);
				return subject2.compareTo(subject1);
			});
			break;
		case 2:
			tableModel.getDataVector().sort((row1, row2) -> {
				String subject1 = (String) row1.get(1);
				String subject2 = (String) row2.get(1);
				return subject1.compareTo(subject2);
			});
			break;
		case 3:
			tableModel.getDataVector().sort((row1, row2) -> {
				double amount1 = (double) row1.get(2);
				double amount2 = (double) row2.get(2);
				return Double.compare(amount1, amount2);
			});
			break;
		case 4:
			tableModel.getDataVector().sort((row1, row2) -> {
				double amount1 = (double) row1.get(2);
				double amount2 = (double) row2.get(2);
				return Double.compare(amount2, amount1);
			});
			break;
		case 5:
			tableModel.getDataVector().sort((row1, row2) -> {
				Date date1 = (Date) row1.get(0);
				Date date2 = (Date) row2.get(0);
				return date2.compareTo(date1);
			});
			break;
		case 6:
			tableModel.getDataVector().sort((row1, row2) -> {
				Date date1 = (Date) row1.get(0);
				Date date2 = (Date) row2.get(0);
				return date1.compareTo(date2);
			});
			break;
		default:
			return;
		}

		tableModel.fireTableDataChanged();
	}

	private String getPassword(String username) {
		JPasswordField passwordField = new JPasswordField();
		passwordField.requestFocusInWindow();
		int result = JOptionPane.showOptionDialog(this,
				new Object[] { "Username: " + username, "Password:", passwordField }, "Login",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

		if (result == JOptionPane.OK_OPTION) {
			char[] passwordChars = passwordField.getPassword();
			return new String(passwordChars);
		} else {
			loginCancelled = true;
			System.exit(0);
		}

		return null;
	}

	private void loadSettings() {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SETTINGS_FILE_PATH))) {
			username = (String) in.readObject();
			password = (String) in.readObject();
		} catch (IOException | ClassNotFoundException e) {

		}
	}

	private String getPassword() {
		JPasswordField passwordField = new JPasswordField();

		int result = JOptionPane.showOptionDialog(this, new Object[] { "Enter password:", passwordField }, "Password",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

		if (result == JOptionPane.OK_OPTION) {
			return new String(passwordField.getPassword());
		} else {
			return null;
		}
	}

	private void createAccount() {
		String newUsername = JOptionPane.showInputDialog(this, "Create username:");
		String newPassword = getPassword();

		if (newUsername != null && newPassword != null) {
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SETTINGS_FILE_PATH))) {
				out.writeObject(newUsername);
				out.writeObject(newPassword);
				this.username = newUsername;
				this.password = newPassword;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error creating account.", "Account Creation Failed",
						JOptionPane.ERROR_MESSAGE);
				createAccount();
			}
		} else {
			JOptionPane.showMessageDialog(this, "Invalid username or password.", "Account Creation Failed",
					JOptionPane.ERROR_MESSAGE);
			createAccount();
		}
	}

	private void loadSpendings() {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SPENDINGS_FILE_PATH))) {
			@SuppressWarnings("unchecked")
			List<Object[]> spendings = (List<Object[]>) in.readObject();

			for (Object[] spending : spendings) {
				tableModel.addRow(spending);
			}

		} catch (IOException | ClassNotFoundException e) {

		}
	}

	private void saveSpendings() {
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SPENDINGS_FILE_PATH))) {
			List<Object[]> spendings = new ArrayList<>();

			for (int row = 0; row < tableModel.getRowCount(); row++) {
				Date date = (Date) tableModel.getValueAt(row, 0);
				String subject = (String) tableModel.getValueAt(row, 1);
				double amount = (double) tableModel.getValueAt(row, 2);

				Object[] spending = { date, subject, amount };

				spendings.add(spending);
			}

			out.writeObject(spendings);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error saving spendings.", "Save Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addSpendings() {
		Date date = new Date();
		String subject = JOptionPane.showInputDialog(this, "Enter subject:");
		String amountString = JOptionPane.showInputDialog(this, "Enter amount:");

		if (subject != null && amountString != null) {
			try {
				double amount = Double.parseDouble(amountString);
				Object[] rowData = { date, subject, amount };
				tableModel.addRow(rowData);
				saveSpendings();
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Invalid amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
				addSpendings();
			}
		}
	}

	private void editSpendings() {
		int selectedRow = table.getSelectedRow();

		if (selectedRow >= 0) {
			String subject = (String) table.getValueAt(selectedRow, 1);
			double amount = (double) table.getValueAt(selectedRow, 2);

			String newSubject = JOptionPane.showInputDialog(this, "Enter new subject:", subject);
			String newAmountString = JOptionPane.showInputDialog(this, "Enter new amount:", amount);

			if (newSubject != null && newAmountString != null) {
				try {
					double newAmount = Double.parseDouble(newAmountString);
					table.setValueAt(newSubject, selectedRow, 1);
					table.setValueAt(newAmount, selectedRow, 2);
					saveSpendings();
				} catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(this, "Invalid amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
					editSpendings();
				}
			}
		} else {
			JOptionPane.showMessageDialog(this, "No spending record selected.", "No Selection",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void quit() {
		if (loginCancelled) {
			System.exit(0);
		} else if (!loggedIn) {
			System.exit(0);
		} else {
			int option = JOptionPane.showConfirmDialog(this, "Save spendings before quitting?", "Quit",
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (option == JOptionPane.YES_OPTION) {
				saveSpendings();
				loadSettings();
				System.exit(0);
			} else if (option == JOptionPane.NO_OPTION) {
				loadSettings();
				System.exit(0);
			}
		}
	}

	private void scheduleNotification() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 21); // Set the hour to 21 (9 PM)
		calendar.set(Calendar.MINUTE, 0); // Set the minute to 0

		long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();
		if (initialDelay < 0) {
			// If the current time is already past 9 PM, calculate the delay for the next
			// day
			Calendar tomorrow = Calendar.getInstance();
			tomorrow.add(Calendar.DAY_OF_YEAR, 1);
			tomorrow.set(Calendar.HOUR_OF_DAY, 21); // Set the hour to 21 (9 PM)
			tomorrow.set(Calendar.MINUTE, 0); // Set the minute to 0
			initialDelay = tomorrow.getTimeInMillis() - System.currentTimeMillis();
		}

		executor.scheduleAtFixedRate(this::showNotification, initialDelay, TimeUnit.DAYS.toMillis(1),
				TimeUnit.MILLISECONDS);
	}

	private void showNotification() {
		JOptionPane.showMessageDialog(this, "Record your spendings!", "Spending Tracker",
				JOptionPane.INFORMATION_MESSAGE);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SpendingTrackerGUI app = new SpendingTrackerGUI();
				app.pack();
				app.setLocationRelativeTo(null);
				app.setVisible(true);
				app.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				app.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						app.quit();
					}
				});

			}
		});
	}
}