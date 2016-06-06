package bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import BankRMI.RejectedException;

@SuppressWarnings("serial")
public class BankImpl extends UnicastRemoteObject implements Bank {
	private String bankName;
	private Map<String, Account> accounts = new HashMap<String, Account>();
        private Statement sql;
	private String ds;
	private String dbms;
        private Connection connect;

    public BankImpl(String bankName, String ds, String dbms) throws RemoteException {
        super();
        this.bankName = bankName;
        this.ds = ds;
        this.dbms = dbms;
        createDatabase();
        connectDatabase();
    }
        
    private void createDatabase() {
    try {
            connect = getConnection(); 
            sql = connect.createStatement();
            boolean ifTable = false;
            int tableColumn = 3;
            StringTokenizer st = new StringTokenizer(bankName, ".");
            String nameOfBank = st.nextToken();

            DatabaseMetaData dmd = connect.getMetaData();
            for (ResultSet rs = dmd.getTables(null, null, null, null); rs.next();) {

                if (rs.getString(tableColumn).equalsIgnoreCase(nameOfBank)) {
                            ifTable = true;
                            rs.close();
                            break;
                    }
            }
            if (!ifTable) {
                    sql.executeUpdate( "CREATE TABLE " + bankName + " (name VARCHAR(32) PRIMARY KEY, balance FLOAT)");
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }
        
    private Connection getConnection() throws ClassNotFoundException, SQLException {

         if (dbms.equalsIgnoreCase("derby")) {
                Class.forName("org.apache.derby.jdbc.ClientDriver");
                return DriverManager.getConnection("jdbc:derby://localhost:1527/" + ds + ";create=true", "ahad", "ahad");        
         } else {
                return null;
         }
    }    
        
    private void connectDatabase() {        
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            String url = "jdbc:derby://localhost:1527/BANKS;";
            connect = DriverManager.getConnection(url, "ahad", "ahad");
            connect.setAutoCommit(false);
        } catch (Exception ex) {
            Logger.getLogger(BankImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    @Override
    public synchronized String[] listAccounts() {
        String[] acc = null;
        int rowCount = 0;
        int index = 0;
        try {
            Statement st = connect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet r = st.executeQuery("SELECT NAME from BANKS.NORDEA");
            r.last();                                   // go to the last row of variable 'r'
            rowCount = r.getRow();                      // counting the row in variable 'r' 
            acc = new String[rowCount];          // declare an array of item type = row number to insert items
            r.beforeFirst();                            // bring the cursor before the first row 'r'
            while (r.next() && index < rowCount) {
                acc[index] = r.getString("NAME");
                index++;

            }
            st.close();
            r.close();
        } catch (SQLException e) {
            System.out.println("Error in Itemlist " + e.getMessage());
        }

        return acc;
       
        //return accounts.keySet().toArray(new String[1]);
                
    }

    @Override
    public synchronized Account newAccount(String name) throws RemoteException, RejectedException {
        AccountImpl account = (AccountImpl) accounts.get(name);
        if (account != null) {
                System.out.println("Account [" + name + "] exists!!!");
                throw new RejectedException("Rejected: Bank: " + bankName +
                                " Account for: " + name + " already exists: " + account);
        }
        
        ResultSet result = null;
        try {
                result = sql.executeQuery("SELECT * from " + bankName + " WHERE NAME='" + name + "'");
                if (result.next()) {
                        // account exists, instantiate, put in cache and throw exception.
                        account = new AccountImpl(name, bankName, result.getFloat("balance"),getConnection());
                        accounts.put(name, account);
                        throw new RejectedException("Rejected: Account for: " + name + " already exists");
                }
                result.close();

                // create account.
                int r = sql.executeUpdate("INSERT INTO " + bankName + " VALUES ('" + name + "', 0)");
                if (r == 1) {
                        account = new AccountImpl(name, bankName, getConnection());
                        accounts.put(name, account);
                        System.out.println("Bank: " + bankName + " Account: " + account +
                                        " has been created for " + name);
                        return account;
                } else {
                        throw new RejectedException("Cannot create an account for " + name);
                }
        } catch (SQLException e) {
                System.out.println(e);
                throw new RejectedException("Cannot create an account for " + name, e);
        }
        catch (ClassNotFoundException e) {
                System.out.println(e);
                throw new RejectedException("Cannot create an account for " + name, e);
        }
    }

    @Override
    public synchronized Account getAccount(String name) throws RemoteException {
	if (name == null) {
            return null;
	}
        Account account =  accounts.get(name);
        if (account == null) {
                try {
                        ResultSet result = sql.executeQuery("SELECT * FROM " + bankName + " WHERE name ='" + name +"'");
                        if (result.next()) {
                            try {
                                account = new AccountImpl(result.getString("name"), bankName, result.getFloat("balance"), getConnection());
                            } catch (RejectedException ex) {
                                Logger.getLogger(BankImpl.class.getName()).log(Level.SEVERE, null, ex);
                            }
                                result.close();
                                accounts.put(name, account);
                        } else {
                                return null;
                        }
                } catch (SQLException e) {
                try {
                    throw new RejectedException("Unable to find account for " + name, e);
                } catch (RejectedException ex) {
                    Logger.getLogger(BankImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
                }
                catch (ClassNotFoundException e) {
                    System.out.println(e);
                try {
                    throw new RejectedException("Cannot create an account for " + name, e);
                } catch (RejectedException ex) {
                    Logger.getLogger(BankImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
                }
        }
        return account;
    }
	
    @Override
    public synchronized boolean deleteAccount(String name) {
        if (!hasAccount(name)) {
            return false;
	}
	accounts.remove(name);
	try {
            int r = sql.executeUpdate("DELETE FROM " + bankName + " WHERE name='" + name + "'");
            if (r != 1) {
                try {
                    throw new RejectedException("Unable to delete account..." + name);
                } catch (RejectedException ex) {
                    Logger.getLogger(BankImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
	} catch (SQLException e) {
            System.out.println("Unable to delete account for " + name + ": " + e.getMessage());
            try {
                 throw new RejectedException("Unable to delete account..." + name, e);
            } catch (RejectedException ex) {
                 Logger.getLogger(BankImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
        System.out.println("Bank: " + bankName + " Account for " + name + " has been deleted");
        return true;
    }

    private boolean hasAccount(String name) {
        if (accounts.get(name) == null) {
                return false;
        } else {
                return true;
        }
    }
}
