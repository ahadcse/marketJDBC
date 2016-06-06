/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package customer;

import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 *
 * @author ahad
 */
public interface CustomerInt extends Remote{
    public void Sold (String s) throws RemoteException;
    public void Choice(String s) throws RemoteException;   
}
