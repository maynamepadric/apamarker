import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new Game(this));
    }

    class Game extends View {
        float bx = 200, by = 200, vx = 9, vy = 9;
        int skor = 0;
        Paint p = new Paint();
        Game(Context c) { super(c); p.setTextSize(44); }
        @Override protected void onDraw(Canvas c) {
            c.drawColor(0xFF101216);
            bx += vx; by += vy;
            if (bx < 24 || bx > getWidth() - 24) vx = -vx;
            if (by < 24) vy = -vy;
            if (by > getHeight() - 24) { vy = -vy; skor = 0; }
            p.setColor(Color.WHITE);
            c.drawCircle(bx, by, 24, p);
            p.setColor(0xFF4CAF50);
            c.drawText("Skor: " + skor + "  -  topa dokun!", 30, 70, p);
            invalidate();
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (Math.hypot(e.getX() - bx, e.getY() - by) < 70) {
                skor++;
                vx *= 1.06f;
                vy = -Math.abs(vy) * 1.06f;
            }
            return true;
        }
    }
}
