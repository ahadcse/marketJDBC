/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package customer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
/**
 *
 * @author ahad
 */
public class Customer extends UnicastRemoteObject implements CustomerInt{
    
        private String name;
        public Customer(String name) throws RemoteException{
        super();
        this.name = name;
    }
    
    @Override
    public void Sold(String s) throws RemoteException{
    
        System.out.println(" Market's Message:" + s);
    }
    
    @Override
    public void Choice(String s) throws RemoteException{
    
        System.out.println("Market's Message: " + s);
    }

}
