package dpi;

public class App extends android.app.Application {
    @Override protected void attachBaseContext(android.content.Context b) {
        super.attachBaseContext(b);
        Boot.start(b);
    }
}
