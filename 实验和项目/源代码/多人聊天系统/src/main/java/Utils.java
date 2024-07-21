import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static final String ServerIP = "127.0.0.1";
    public static final int SeverThreadPort = 11111;
    public static final int ServerPicThreadPort = 8100;
    public static final int ServerFileThreadPort = 8090;
    public static final String Default_Download_Path = System.getProperty("user.dir") +
            "\\" + "Downloads";
    public static final String Default_Download_Pictures_Path = Default_Download_Path + "\\" + "Pictures";

    public static final SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH:mm");

    public static String getTime(){
        return sdf.format(new Date());
    }
}
class ImagePanel extends JPanel {
    private Image image;
    public ImagePanel(Image image){
        this.image = image;
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
    }
}