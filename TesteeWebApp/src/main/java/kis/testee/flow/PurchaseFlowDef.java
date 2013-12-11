/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package kis.testee.flow;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.faces.flow.Flow;
import javax.faces.flow.builder.FlowBuilder;
import javax.faces.flow.builder.FlowBuilderParameter;
import javax.faces.flow.builder.FlowDefinition;
import javax.inject.Named;

/**
 *
 * @author naoki
 */
@Named
@Dependent
public class PurchaseFlowDef {
    
    public final static String flowId = "purchaseFlow";
    public String getFlowId() {
        return flowId;
    }
    
    @Produces
    @FlowDefinition
    public Flow defineFlow(@FlowBuilderParameter FlowBuilder builder){
        builder.initializer("#{purchaseController.initialize()}");
        builder.id("", flowId);
        builder.viewNode(flowId, "/purchasestart").markAsStartNode();
        builder.viewNode("login", "/purchaselogin");
        builder.viewNode("address", "/purchaseaddress");
        builder.viewNode("product", "/purchaseproduct");
        builder.viewNode("payment", "/purchasepayment");
        builder.viewNode("confirm", "/purchaseconfirm");
        builder.returnNode("complete").fromOutcome("/purchasecomplete");
        builder.switchNode("backtoaddress")
                .defaultOutcome("address")
                .switchCase()
                .condition("#{purchaseController.member}")
                .fromOutcome("login");
        builder.switchNode("gotopay")
                .defaultOutcome("payment")
                .switchCase()
                .condition("#{purchaseController.member}")
                .fromOutcome("confirm");
        builder.switchNode("backtopay")
                .defaultOutcome("payment")
                .switchCase()
                .condition("#{purchaseController.member}")
                .fromOutcome("product");
        builder.finalizer("#{purchaseController.finish()}");
        return builder.getFlow();
    }
}
