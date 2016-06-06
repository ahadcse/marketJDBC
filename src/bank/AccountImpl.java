package bank;

import BankRMI.RejectedException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@SuppressWarnings("serial")
public class AccountImpl extends UnicastRemoteObject implements Account {
    private float balance;
    private String name;
    private String bank;
    private Connection connect;
    private PreparedStatement updateSQL;
    
    /** 
     * Constructs a persistently named object. 
     */
    public AccountImpl(String name, String bank, float balance, Connection connect) throws RemoteException, RejectedException {
        super();
        this.name = name;
        this.bank = bank;
        this.balance = balance;
        this.connect = connect;
        try {
            updateSQL = connect.prepareStatement("UPDATE " + bank + " SET balance=? WHERE name='" + name + "'");
        } catch (SQLException sqle) {
                    throw new RejectedException("Unable to instantiate account", sqle);
        }
    }
    
    public AccountImpl(String name1, String bank, Connection connect) throws RemoteException, RejectedException {
        this(name1, bank, 0, connect);
    }
    
    public synchronized void deposit(float value) throws RemoteException, RejectedException {
        if (value < 0) {
            throw new RejectedException("Rejected: Account " + name + 
            		                    ": Illegal value: " + value);
        }
        boolean success = false;
		try {
                    balance += value;
                    updateSQL.setDouble(1, balance);
                    int r = updateSQL.executeUpdate();
                    if (r != 1) {
                            throw new RejectedException("Unable to deposit into account: " + name);
                    } else {
                            success = true;
                    }
                    System.out.println("Transaction: Account " + name + ": deposit: $" + value + ", balance: $" + balance);
		} catch (SQLException sqle) {
                    throw new RejectedException("Unable to deposit into account: " + name, sqle);
		} finally {
			if (!success) {
                            balance -= value;
			}
		}
    }   
        
    @Override
    public synchronized void withdraw(float value) throws RemoteException,RejectedException {
	if (value < 0) {
            throw new RejectedException("Rejected: Account " + name + ": Illegal value: " + value);
	}
	if ((balance - value) < 0) {
            throw new RejectedException("Rejected: Account " + name + ": Negative balance on withdraw: " + (balance - value));
	}
        boolean success = false;
        try {
            balance -= value;
            updateSQL.setDouble(1, balance);
            int rows = updateSQL.executeUpdate();
            if (rows != 1){
                throw new RejectedException("Unable to deposit into account: " + name);
            } else {
            	success = true;
            }
            System.out.println("Transaction: Account " + name + ": deposit: $" +
                    value + ", balance: $" + balance);
        } catch (SQLException sqle) {
            throw new RejectedException("Unable to deposit into account: " + name, sqle);
        } finally {
            if (!success) {
                balance += value;
            }
        }
    }
    
    @Override
    public synchronized float getBalance() throws RemoteException {
            return balance;
    }
 }
  
