/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package market;

import BankRMI.RejectedException;
import java.util.*;
import customer.*;
import java.rmi.*;
import bank.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;
/**
 *
 * @author ahad
 */
public class Market extends UnicastRemoteObject implements MarketInt{
    
    private String marketName = "Sweden";
    private Map<String, CustomerInt> customer = new HashMap<String, CustomerInt>();
    private Map<String, Product> ProductList = new HashMap<String, Product>();
    private Map<String, Product> ChoiceList = new HashMap<String, Product>();
    private static final long serialVersionUID = 7526472295622776147L;
    Bank bankObj;
    CustomerInt custObj;
    private Connection connect;
    //private String name;
    
    public Market(String name) throws RemoteException, ClassNotFoundException, SQLException {
        super();
        this.marketName = name;
        connectDatabase();
    }
    
    @Override
    public boolean register(String name, String passwd)throws RemoteException{
      try {
            Statement st = connect.createStatement();
            int res = st.executeUpdate("INSERT INTO MARKET.CUSTOMER (CUSTNAME, CUSTPASS, TOTITEMSSOLD, TOTITEMSBOUGHT) VALUES ('" + name + "', '" + passwd + "', 0, 0)");
            if (res > 0) {
                connect.commit();
                st.close();
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e);
            return false;
        }       
    } 
    
    @Override
    public boolean unregister(String name) throws RemoteException{
        try {
            Statement st = connect.createStatement();
            int r = st.executeUpdate("DELETE FROM MARKET.CUSTOMER WHERE CUSTNAME = '" + name + "'");

            if (r != 0) {
                connect.commit();
                st.close();
                System.out.println("Client " + name + " has been Unregistered from the Market");
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Unable to Unregister  account for " + name + ": "
                    + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean login(String uname, String password) throws RemoteException {
        boolean validUser = false;
        try {

            Statement st = connect.createStatement();
            ResultSet r = st.executeQuery("SELECT * from MARKET.CUSTOMER WHERE CUSTNAME='" + uname + "'");
            while (r.next()) {
                if (password.equals(r.getString("CUSTPASS"))) {
                    validUser = true;
                } else {
                    validUser = false;
                }
            }
            st.close();

        } catch (SQLException e) {
            System.out.println("Customer not found on DB" + e);
        }
        return validUser;
    }
    
    @Override
    public Product[] getProduct() throws RemoteException {
        Product[] productArray = null;
        int rowCount = 0;
        int index = 0;
        try {
            Statement st = connect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet r = st.executeQuery("SELECT * FROM MARKET.ITEMLIST");
            r.last();                                   // go to the last row of variable 'r'
            rowCount = r.getRow();                      // counting the row in variable 'r' 
            productArray = new Product[rowCount];          // declare an array of item type = row number to insert items
            r.beforeFirst();                            // bring the cursor before the first row 'r'
            while (r.next() && index < rowCount) {
                String owner = r.getString("ITEMOWNER");
                String productName = r.getString("ITEMNAME");
                float price = r.getFloat("ITEMPRICE");
                productArray[index] = new Product(owner, productName, price);
                index++;
            }
            st.close();
            r.close();
        } catch (SQLException e) {
            System.out.println("Error in Itemlist " + e.getMessage());
        }

        return productArray;
    }
    
    @Override
    public Product[] getChoice() throws RemoteException{
        // more or less same logic of the previous method
        Product[] productArray = null;
        int rowCount = 0;
        int index = 0;
        try {
            Statement stmt = connect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet res = stmt.executeQuery("SELECT * FROM MARKET.WISHLIST");

            res.last();
            rowCount = res.getRow();
            productArray = new Product[rowCount];
            res.beforeFirst();
            while (res.next() && index < rowCount) {
                String itemOwner = res.getString("ITEMOWNER");
                String itemName = res.getString("ITEMNAME");
                float price = res.getFloat("ITEMPRICE");
                productArray[index] = new Product(itemOwner, itemName, price);
                index++;
            }
            stmt.close();
            res.close();
        } catch (SQLException e) {
            System.out.println("Error in wishlist " + e.getMessage());
        }

        return productArray;
    }
    
    @Override
    public boolean adChoice(String buyer, String itemName, float price) throws RemoteException{   
        try {
            Statement st = connect.createStatement();
            int r = st.executeUpdate("INSERT INTO MARKET.WISHLIST (ITEMNAME, ITEMPRICE, ITEMOWNER, ITEMAMOUNT) VALUES ('" + itemName + "', " + price + ", '" + buyer + "', 0)");

            if (r > 0) {
                connect.commit();

                st.close();
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e);
            return false;
        }
    }
    
    public void display(Map list, String type) {
        Set set = list.entrySet();
        Iterator i = set.iterator();
        System.out.printf("Current %s view\nName\tPrice\tOwner\n-------------------------------------\n", type);
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            Product pr = ProductList.get((String) me.getKey());
            System.out.printf("%s\t%s\t%s\n", pr.getProductName(), pr.getProductPrice(), pr.getOwner());
        }
    }
    
    @Override
    public Product buyProduct(String buyer, String owner, String itemName, float price) throws RemoteException{
    
        //check the bank account to see whether the buyer has enough money in his bank to buy this item
        bankConnection("BANKS.Nordea");  // this is connected to RMI
        boolean success = withdrawBalance(buyer, price);
        Product p = new Product();
        if (success) {   // if the money can be withdrawn
            try {
                Statement st = connect.createStatement();
                ResultSet r = st.executeQuery("SELECT * from MARKET.ITEMLIST WHERE ITEMOWNER='" + owner + "' AND ITEMNAME='" + itemName + "'");
                // select the bought item from the database
                while (r.next()) {
                    String productName = r.getString("ITEMNAME");
                    float productPrice = r.getFloat("ITEMPRICE");
                    String productOwner = r.getString("ITEMOWNER");
                    p = new Product(productOwner, productName, productPrice); // sent product info to Product class
                    p.setOwner(buyer);     // changing owner name
                    ResultSet restot = st.executeQuery("SELECT TOTITEMSBOUGHT FROM MARKET.CUSTOMER WHERE CUSTNAME = '" + buyer + "'");
                    // find the no of rows to see how many items he bought
                    restot.next();
                    int totalItemBought = restot.getInt("TOTITEMSBOUGHT"); // get the no of rows in int
                    st.executeUpdate("UPDATE MARKET.CUSTOMER SET TOTITEMSBOUGHT = " + (totalItemBought + 1) + " WHERE CUSTNAME = '" + buyer + "'");
                    int row = st.executeUpdate("DELETE FROM MARKET.ITEMLIST WHERE ITEMOWNER='" + owner + "' AND ITEMNAME='" + itemName + "'");
                    // delete the item from the market view because it is now bought
                    if (row > 0) {
                        connect.commit();
                        System.out.println("Item deleted from ITEMLIST: " + productName);
                    }
                }
                st.close();
            } catch (SQLException se) {
                System.out.println("SQL Error: " + se.getMessage());
                p = new Product("", "", 0);
                return p;
            }
            try {
                depositBalance(owner, price); // giving money to the previous owner
            } catch (RejectedException ex) {
                Logger.getLogger(Market.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                custObj = (CustomerInt) Naming.lookup(owner);
                custObj.Sold("Your item " + itemName + " is sold to " + buyer);
               
            } catch (Exception e) {
                System.out.println("send notification failed" + e.getMessage());
            }
            return p;

        } else {
            System.out.println("Not enough balance to buy this item");
            p = new Product("", "", 0);
            return p;
        }
    }
    
    @Override
    public boolean sellProduct(String seller, String itemName, float price) throws RemoteException{
    
        try {
            Statement st = connect.createStatement();
            int r = st.executeUpdate("INSERT INTO MARKET.ITEMLIST (ITEMNAME, ITEMPRICE, ITEMOWNER, ITEMAMOUNT) VALUES ('" + itemName + "', " + price + ", '" + seller + "', 0)");
            if (r > 0) {
                ResultSet restotal = st.executeQuery("SELECT TOTITEMSSOLD FROM MARKET.CUSTOMER WHERE CUSTNAME = '" + seller + "'");
                restotal.next();  // get total row number to know how many item in TOTITEMSSOLD
                int totalItemSold = restotal.getInt("TOTITEMSSOLD"); // convert it to integer to know the row number
                st.executeUpdate("UPDATE MARKET.CUSTOMER SET TOTITEMSSOLD = " + (totalItemSold + 1) + " WHERE CUSTNAME = '" + seller + "'");
                connect.commit(); // to complete all trasection of database in correct sequence 
                ResultSet result = st.executeQuery("SELECT * FROM MARKET.WISHLIST WHERE ITEMNAME = '" + itemName + "' AND ITEMPRICE >= " + price + " ");
                //?? have to understand what this SQL for
                while (result.next()) {
                    custObj = (CustomerInt) Naming.lookup(result.getString("ITEMOWNER"));
                    custObj.Choice(result.getString("ITEMOWNER") + ",Item " + result.getString("ITEMNAME") + " is available at price:" + price);
                }
                r = st.executeUpdate("DELETE FROM MARKET.WISHLIST WHERE ITEMNAME = '" + itemName + "' AND ITEMPRICE >= " + price + " ");
                connect.commit();
                st.close();
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println("SQL failed" + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("send notification failed" + e.getMessage());
            return false;
        }
    }
    
    public void bankConnection(String bankName) throws RemoteException{
        try{
            bankObj = (Bank)Naming.lookup(bankName);
            if(bankObj != null){
                System.out.println("Connected to Bank" + bankName);
          }
        }catch(Exception ex){
            System.out.println("Connection failed");
            System.out.println(ex);
        }
    }
    
    public boolean withdrawBalance(String buyer, float price){        
        try{
            Account ac = bankObj.getAccount(buyer);
            if(ac == null){
                System.out.println("No account for" + buyer);
                return false;
            }
            ac.withdraw(price);
            float balance = ac.getBalance();
            System.out.printf("Amount %f withdrawn from %s's account and new balance is %f\n", price, buyer, balance);
            return true;   
        }catch (RemoteException re) {
            System.out.println("Remote exception: " + re.getMessage());
            return false;
        } catch (RejectedException rj) {
            System.out.println("Transaction rejected by the server" + rj.getMessage());
            return false;
        }
    }
    
    public void depositBalance(String owner, float price) throws RejectedException{
    
        try{
            Account ac = bankObj.getAccount(owner);
            if(ac == null){
                System.out.println("No account for" + owner);
            }
            ac.deposit(price);
            float balance = ac.getBalance();
            System.out.printf("Amount %f deposited to %s's account and new balance is %f\n", price, owner, balance);   
        }catch (RemoteException re) {
            System.out.println("Remote exception: " + re.getMessage());
        } catch (RejectedException rj) {
            System.out.println("Transaction rejected by the server" + rj.getMessage());
        }
    }
    
    @Override
    public int getTotalItemSold (String owner) throws RemoteException {
        
        int iTotalItemSold = -999;
        try {
            Statement stmt = connect.createStatement();
            ResultSet restotal = stmt.executeQuery("SELECT TOTITEMSSOLD FROM MARKET.CUSTOMER WHERE CUSTNAME = '" + owner + "'");
            restotal.next();
            iTotalItemSold = restotal.getInt("TOTITEMSSOLD");
            restotal.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL failed" + e.getMessage());
        }
        return iTotalItemSold;
    }
    
    @Override
    public int getTotalItemBought (String owner) throws RemoteException {        
        int iTotalItemBought = -999;
        try {
            Statement stmt = connect.createStatement();
            ResultSet restotal = stmt.executeQuery("SELECT TOTITEMSBOUGHT FROM MARKET.CUSTOMER WHERE CUSTNAME = '" + owner + "'");
            restotal.next();
            iTotalItemBought = restotal.getInt("TOTITEMSBOUGHT");
            restotal.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("SQL failed" + e.getMessage());
        }        
        return iTotalItemBought;
    }
    
    private void connectDatabase() throws ClassNotFoundException, SQLException {        
        Class.forName("org.apache.derby.jdbc.ClientDriver");
        String url = "jdbc:derby://localhost:1527/MARKET;";
        connect = DriverManager.getConnection(url, "ahad", "ahad");
        connect.setAutoCommit(false);
    }    
}