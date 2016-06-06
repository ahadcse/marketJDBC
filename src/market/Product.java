/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package market;

import java.io.Serializable;

/**
 *
 * @author ahad
 */
public class Product implements Serializable{
    
    private String owner;
    private String productName;
    private float price;
    private static final long serialVersionUID = 7526472295622776147L; 
    public Product(){
        this.owner = "";
        this.productName = "";
        this.price = 0;
    }
    
    public Product(String owner, String productName, float price){
        
        this.owner = owner;
        this.productName = productName;
        this.price = price;   
    }
    
    public void setOwner(String owner){
        
        this.owner = owner;
    }
    
    public String getOwner(){
        
        return owner;
    }
    
    public String getProductName(){
    
        return productName;    
    }
    
    public float getProductPrice(){
    
        return price;
    }
}
