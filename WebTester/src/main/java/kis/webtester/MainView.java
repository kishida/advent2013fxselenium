package kis.webtester;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import static jdk.nashorn.internal.runtime.Debug.id;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLElement;

/**
 * Hello world!
 *
 */
public class MainView extends Application{
    public static void main( String... args ){
        setup(args);
        load("http://localhost:8080/TesteeWebApp/");
        try {
            Thread.sleep(500);
            HTMLElement elm = findByText("開始画面");
            System.out.println(elm);
        } catch (InterruptedException ex) {
        }
        
    }

    static WebEngine engine;

    @Override
    public void start(Stage stage) throws Exception {
        WebView wv = new WebView();
        engine = wv.getEngine();
        engine.getLoadWorker().stateProperty().addListener((ov, t, t1) -> {
            if(t1.equals(Worker.State.SUCCEEDED)){
                stage.setTitle(engine.getTitle());
            }
        });
        
        Scene scene = new Scene(wv, 600, 400);
        
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
    
    static <T, R> R withFx(T arg, BiConsumer<T, BlockingQueue<R>> cons){
        final BlockingQueue<T> param = new LinkedBlockingQueue<>();
        final BlockingQueue<R> ret = new LinkedBlockingQueue<>();

        Platform.runLater(() -> {
            try {
                T u = param.take();
                cons.accept(u, ret);
            } catch (InterruptedException ex) {
            }
        });
        R take = null;
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
    
    public static Worker.State load(String url){
        return withFx(url, (u, ret) -> {
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
}
