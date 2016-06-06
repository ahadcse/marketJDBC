/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bank;

import BankRMI.RejectedException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    public float getBalance() throws RemoteException;
    public void deposit(float value) throws RemoteException, RejectedException;
    public void withdraw(float value) throws RemoteException, RejectedException;
}
