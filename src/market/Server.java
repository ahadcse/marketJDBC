/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package market;

/**
 *
 * @author ahad
 */
public class Server {
    private static final String MARKET = "Sweden";
     public Server(String name){
         try{
                MarketInt market = new Market(name);
                java.rmi.Naming.rebind(name, market);
                System.out.println(market + "is ready");
         }
         catch(Exception ex){
                System.out.println(ex);
         }
    }
    
    public static void main(String[] args){
        
        new Server("Sweden");
    
    }
}
