package kis.webtester;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import jdk.nashorn.api.scripting.JSObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLButtonElement;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLInputElement;

/**
 * Hello world!
 *
 */
public class MainView extends Application{
    public static void main( String... args ){
        setup(args);
        try {
            String path = "C:\\Users\\naoki\\Desktop\\fxadvent2013\\";
            Thread.sleep(300);
            load("http://localhost:8080/TesteeWebApp/");
            Thread.sleep(300);
            HTMLElement elm = findByText("開始画面");
            System.out.println(elm);
            capture(path + "01index.png");
            clickById("fm:btnStart");
            Thread.sleep(300);
            capture(path + "02start.png");
            clickById("fm:btnLogin");
            Thread.sleep(300);
            setValueById("fm:txtUsername", "kishida");
            setValueById("fm:txtPassword", "naoki");
            capture(path + "03logininput.png");
            clickById("fm:btnLogin");
            Thread.sleep(300);
            capture(path + "04loginfail.png");
            setValueById("fm:txtPassword", "kishida");
            clickById("fm:btnLogin");
            Thread.sleep(300);
            clickById("fm:tblProduct:1:btnSelect");
            setValueById("fm:txtAmount", "4");
            clickById("fm:btnNext");
        } catch (Exception ex) {
        }
        
    }

    static WebEngine engine;
    static Stage mainStage;
    
    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;
        WebView wv = new WebView();
        engine = wv.getEngine();
        engine.getLoadWorker().stateProperty().addListener((ov, t, t1) -> {
            if(t1.equals(Worker.State.SUCCEEDED)){
                stage.setTitle(engine.getTitle());
            }
        });
        
        Scene scene = new Scene(wv, 400, 400);
        
        stage.setTitle("Hello");
        stage.setScene(scene);
        stage.show();
        
    }
    static void withLatch(Runnable r){
        final Lock lk = new ReentrantLock();
        final Condition cond = lk.newCondition();
        Platform.runLater(()-> {
            r.run();
            lk.lock();
            try {
                cond.signal();
            } finally {
                lk.unlock();
            }
        });
        lk.lock();
        try {
            cond.await();
        } catch (InterruptedException ex) {
        } finally {
            lk.unlock();
        }
        
    }

    static <R> R withRet(Supplier<R> cons){
        final BlockingQueue<List<R>> ret = new LinkedBlockingQueue<>();

        Platform.runLater(() -> {
            try {
                ret.put(Arrays.asList(cons.get()));
            } catch (InterruptedException ex) {
            }
        });
        R result = null;
        try {
            result = ret.take().get(0);
        } catch (InterruptedException ex) {
        }
        return result;
        
    }
    
    static Worker.State load(String arg){
        final BlockingQueue<String> param = new LinkedBlockingQueue<>();
        final BlockingQueue<Worker.State> ret = new LinkedBlockingQueue<>();

        Platform.runLater(() -> {
            try {
                String u = param.take();
                engine.load(u);
                engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>(){
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> ov, Worker.State t, Worker.State t1) {
                        try{
                            if(t1.equals(Worker.State.SUCCEEDED) || t1.equals(Worker.State.FAILED) || t1.equals(Worker.State.CANCELLED)){
                                ret.put(t1);
                                engine.getLoadWorker().stateProperty().removeListener(this);
                            }
                        }catch(InterruptedException ex){
                        }
                    }
                });
            } catch (InterruptedException ex) {
            }
        });
        Worker.State take = null;
        try {
            param.put(arg);
            take = ret.take();
        } catch (InterruptedException interruptedException) {
        }
        return take;        
    }
    
    public static void setup(String... args){
        new Thread(() -> launch(args)).start();
    }
    
    public static void shutdown(){
        Platform.runLater(() -> {
            Platform.exit();
        });
    }
    
    private static HTMLElement findByTextImpl(Node n, String text) {
        if (n.getTextContent() != null && n.getTextContent().trim().equals(text)) {
            return (HTMLElement) n;
        }
        for (int i = 0; i < n.getChildNodes().getLength(); ++i) {
            HTMLElement ret = findByTextImpl(n.getChildNodes().item(i), text);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }
    public static HTMLElement findByText(String text){
        return withRet(() -> {
            return findByTextImpl(engine.getDocument(), text);
        });
    }
    
    public static void clickById(final String id){
        withLatch(() -> {
            Element elem = engine.getDocument().getElementById(id);
            if(elem != null){
                if(elem instanceof HTMLInputElement){
                    ((HTMLInputElement)elem).click();
                }else if(elem instanceof HTMLButtonElement){
                    Object o = engine.executeScript(
                            "var evt = document.createEvent('MouseEvents');"
                            + "evt.initEvent('click', false, true);evt");
                    ((JSObject)elem).call("dispatchEvent", o);
                }
            }
        });
    }

    public static void setValueById(String id, String value){
        withLatch(() -> {
            Element elem = engine.getDocument().getElementById(id);
            if (elem != null && elem instanceof HTMLInputElement) {
                ((HTMLInputElement) elem).setValue(value);
            }
        });
    }
    
    public static void capture(String filename){
        try {
            Rectangle rect = new Rectangle(
                    (int)mainStage.getX(), 
                    (int)mainStage.getY(), 
                    (int)mainStage.getWidth(), 
                    (int)mainStage.getHeight());
            
            BufferedImage img = new Robot().createScreenCapture(rect);
            ImageIO.write(img, "png", new File(filename));
        } catch (IOException | AWTException ex) {
        }
    }
}
