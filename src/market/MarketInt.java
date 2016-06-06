/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package market;

import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 *
 * @author ahad
 */
public interface MarketInt extends Remote{
    
    public boolean register(String name, String passwd) throws RemoteException;
    public boolean unregister(String name) throws RemoteException;
    public boolean login(String username, String password) throws RemoteException;
    public Product[] getProduct() throws RemoteException;  
    public Product[] getChoice() throws RemoteException;
    public Product buyProduct(String buyer, String owner, String itemName, float price) throws RemoteException;
    public boolean sellProduct(String seller, String itemName, float price) throws RemoteException;
    public boolean adChoice(String buyer, String itemName, float price) throws RemoteException;
    public int getTotalItemSold (String owner) throws RemoteException;
    public int getTotalItemBought(String owner) throws RemoteException;
    
}
