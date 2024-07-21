import net.sf.json.JSONObject;
import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class Login extends JFrame{

    public static void main(String[] args) {
        new Login();
    }

    public Login(){

        setTitle("登录");
        setLayout(null);
        setSize(320,470);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        //logo
        ImageIcon logo = new ImageIcon("media/logo.png");
        setIconImage(logo.getImage());

        //用户头像面板
        ImageIcon avatar = new ImageIcon("media/avatar.png");
        ImagePanel imagePanel = new ImagePanel(avatar.getImage());
        imagePanel.setBounds(100,5,100,100);
        add(imagePanel);

        //界面标题
        JLabel headTitle = new JLabel("登录",SwingConstants.CENTER);
        headTitle.setFont(new Font(null, Font.BOLD, 30));  // 设置文本的字体类型、样式 和 大小
        headTitle.setBounds(100,100,100,50);
        add(headTitle);

        //用户名标题
        JLabel username_label = new JLabel("昵称:");
        username_label.setBounds(50,140,100,50);
        username_label.setFont(new Font(null, Font.PLAIN, 18));
        add(username_label);

        //输入用户名的文本区域
        JTextField username_field = new JTextField();
        username_field.setBounds(50,180,200,35);
        username_field.setFont(new Font(null, Font.PLAIN, 13));
        add(username_field);

        //登录按钮
        JButton login = new JButton("登录");
        login.setBounds(100,290,100,40);
        login.setFont(new Font(null,Font.PLAIN,15));
        login.setBackground(new Color(0,191,255));
        add(login);

        setVisible(true);   //设置是否可见

        //登录按钮点击事件
        login.addActionListener(e -> {
            String username = username_field.getText();

            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("link_type", "attempt_login");

            try {
                Socket socket = new Socket(Utils.ServerIP, Utils.SeverThreadPort);
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF(data.toString());

                String confirm = inputStream.readUTF();
//                System.out.println(confirm);
                if(confirm.equals("accepted")){
//                    System.out.println("昵称有效");
                    setVisible(false);
                    new Client(username);
                }
                else {
                    JOptionPane.showMessageDialog(null,"昵称重复","提示",
                            JOptionPane.WARNING_MESSAGE);
                }
                socket.close();

            } catch (IOException ecp) {
                JOptionPane.showMessageDialog(null,"服务器无响应","提示",
                        JOptionPane.WARNING_MESSAGE);
            }


        });
    }

}



