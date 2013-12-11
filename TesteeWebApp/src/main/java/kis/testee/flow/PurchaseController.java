/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package kis.testee.flow;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.flow.FlowScoped;
import javax.inject.Named;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author naoki
 */
@Named
@FlowScoped(PurchaseFlowDef.flowId)
public class PurchaseController implements Serializable{
    @Setter @Getter
    boolean member;
    
    @Setter @Getter
    @NotNull @Size(min=1)
    String address;
    
    @Setter @Getter
    @NotNull @Size(min=1)
    String shipName;
    
    @Setter @Getter
    @NotNull @Size(min = 1, max=8)
    String userName;
    
    @Setter @Getter 
    @NotNull @Size(min = 1, max=8)
    String password;
    
    @Setter @Getter
    @NotNull
    @Min(1) @Max(10)
    Integer amount;
    
    @Setter @Getter
    Product product;
    
    @AllArgsConstructor
    @Setter @Getter
    @EqualsAndHashCode(of = "name")
    public class Product{
        String name;
        int price;
    }
    
    public List<Product> getProductList(){
        return Arrays.asList(
                new Product("かわのよろい", 50), 
                new Product("もうふのよろい", 150), 
                new Product("だきまくらのたて", 70));
    }
    
    public void initialize(){
        member = false;
        address = "";
        shipName = "";
        userName = "";
        password = "";
        amount = null;
        product = null;
    }
    
    public String guest(){
        member = false;
        return "product";
    }
    
    public String login(){
        if(userName != null && !userName.isEmpty() && userName.equals(password)){
            member = true;
            return "product";
        }else{
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, 
                    "IDとパスワードが一致しません", "IDとパスワードが一致しません"));
            return null;
        }
    }
    
    public void finish(){
        System.out.println("finish!");
    }
}
