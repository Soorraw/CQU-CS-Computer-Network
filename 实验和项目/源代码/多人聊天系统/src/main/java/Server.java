import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import net.sf.json.JSONObject;


//继承JFrame实现可视化
public class Server extends JFrame{

    //用户列表，用于存放连接上的用户信息
    ArrayList<User> user_list = new ArrayList<>();
    //用户名列表，用于显示已连接上的用户
    ArrayList<String> username_list = new ArrayList<>();

    //用户组列表,用于查询已存在的用户组
    ArrayList<String> groupname_list = new ArrayList<>();

    //消息显示区域
    static JTextPane show_pane = new JTextPane();
    static StyledDocument show_area = show_pane.getStyledDocument();
    //用户名显示区域
    JTextArea show_user = new JTextArea(10, 10);

    //socket的数据输出流
    DataOutputStream outputStream = null;
    //socket的数据输入流
    DataInputStream inputStream = null;

    ServerFileThread serverFileThread;

    ServerPicThread serverPicThread;
    //从主函数里面开启服务端
    public static void main(String[] args) {
        new Server();
    }

    //构造函数
    public Server() {
        //文件服务
        serverFileThread = new ServerFileThread();
        serverFileThread.start();

        //图片服务
        serverPicThread = new ServerPicThread();
        serverPicThread.start();

        //设置流式布局
        setLayout(new BorderLayout());
        //VERTICAL_SCROLLBAR_AS_NEEDED设置垂直滚动条需要时出现
        //HORIZONTAL_SCROLLBAR_NEVER设置水平滚动条不出现
        //创建信息显示区的画布并添加到show_area
        JScrollPane panel = new JScrollPane(show_pane,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //设置信息显示区标题
        panel.setBorder(new TitledBorder("信息显示区"));
        //布局到中央
        add(panel,BorderLayout.CENTER);
        //设置信息显示区为不可编辑
        show_pane.setEditable(false);


        //创建用于显示用户的画布
        final JPanel panel_east = new JPanel();
        //添加流式布局
        panel_east.setLayout(new BorderLayout());
        //设置标题
        panel_east.setBorder(new TitledBorder("在线用户"));
        //在用户显示区添加show_user
        panel_east.add(new JScrollPane(show_user), BorderLayout.CENTER);
        //设置用户显示区域为不可编辑
        show_user.setEditable(false);
        //将显示用户的画布添加到整体布局的右侧
        add(panel_east, BorderLayout.EAST);

        //创建关于踢下线用户的画布
        final JPanel panel_south = new JPanel();
        //创建标签
        JLabel label = new JLabel("输入要强制下线用户的ID");
        //创建输入框
        JTextField out_area = new JTextField(40);
        //创建踢下线按钮
        JButton out_btn = new JButton("下线");
        //依次添加进画布
        panel_south.add(label);
        panel_south.add(out_area);
        panel_south.add(out_btn);
        //将踢下线用户的画布添加到整体布局的下侧
        add(panel_south,BorderLayout.SOUTH);

        //设置踢下线按钮的监听
        out_btn.addActionListener(e -> {
            try {
                //用于存储踢下线用户的名字
                String out_username;
                //从输入框中获取踢下线用户名
                out_username = out_area.getText().trim();
                //用于判断该用户是否被踢下线
                boolean is_out=false;
                //遍历用户列表依次判断
                for (int i = 0; i < user_list.size(); i++){
                    //比较用户名，相同则踢下线
                    if(user_list.get(i).getUsername().equals(out_username)){
                        //获取被踢下线用户对象
                        User out_user = user_list.get(i);
                        //使用json封装将要传递的数据
                        JSONObject data = new JSONObject();
                        data.put("username",out_user.getUsername());
                        //封装全体用户名，广播至所有用户
                        data.put("user_list", username_list);
                        //广播的信息内容
                        data.put("link_type", "kick");
                        data.put("channel_type", "private");
                        data.put("channel", "world");
                        String msg = Utils.getTime() + " " + data.getString("username") + " 被强制下线" +"\n";
                        data.put("msg", msg);
                        //服务端消息显示区显示相应信息

                        try { // 插入文本
                            show_area.insertString(show_area.getLength(),"用户 " + out_user.getUsername()
                                    + " 在" + new Date() + " 被强制退出系统"+"\n",null);
                        } catch (BadLocationException be) {
                            be.printStackTrace();
                        }

                        try {
                            outputStream = new DataOutputStream(out_user.getSocket().getOutputStream());
                            //传递信息
                            outputStream.writeUTF(data.toString());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        //将被踢用户移出用户列表
                        user_list.remove(i);
                        serverFileThread.remove(i);
                        serverPicThread.remove(i);
                        //将被踢用户移出用户名列表
                        username_list.remove(out_user.getUsername());
                        //依次遍历用户列表
                        for (User value : user_list) {
                            try {
                                //获取每个用户列表的socket连接
                                outputStream = new DataOutputStream(value.getSocket().getOutputStream());
                                //传递信息
                                outputStream.writeUTF(data.toString());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                        //刷新在线人数
                        show_user.setText("");
                        show_user.setText("人数有 " + username_list.size() + " 人\n");
                        //刷新在线用户
                        for (String s : username_list) {
                            show_user.append(s + "\n");
                        }
                        //判断踢出成功
                        is_out=true;
                        break;
                    }

                }
                //根据是否踢出成功弹出相应提示
                if(is_out){
                    JOptionPane.showMessageDialog(null,"强制下线成功","提示",
                            JOptionPane.WARNING_MESSAGE);
                }
                if(!is_out){
                    JOptionPane.showMessageDialog(null,"不存在用户","提示",
                            JOptionPane.WARNING_MESSAGE);
                }
                //重置输入框
                out_area.setText("");
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        });

        //logo
        ImageIcon logo = new ImageIcon("media/CloudServer.png");
        setIconImage(logo.getImage());

        //设置该窗口名
        setTitle("服务器 ");
        //设置窗体大小
        setSize(700, 500);
        //设置窗体位置可移动
        setLocationRelativeTo(null);
        //设置窗体关闭方式
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //设置窗体可见
        setVisible(true);

        //socket连接相关代码
        try {
            //开启socket服务器，绑定端口11111
            ServerSocket serverSocket = new ServerSocket(Utils.SeverThreadPort);
            //信息显示区打印服务器启动时间

            try { // 插入文本
                show_area.insertString(show_area.getLength(),"服务器启动时间 " + new Date() + "\n",null);
            } catch (BadLocationException be) {
                be.printStackTrace();
            }

            //持续接收连接
            while (true) {
                //接收连接
                Socket socket = serverSocket.accept();
                //创建用户对象
                User user;
                    //获取输入流
                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());

                    //读取输入流
                    String json = inputStream.readUTF();
                    //创建信息对象
                    JSONObject data = JSONObject.fromObject(json);

                    String link_type = data.getString("link_type");
//                    System.out.println(link_type);

                        //创建新用户
                        user = new User();
                        //存储socket对象
                        user.setSocket(socket);
                        //获取输入流用户名
                        user.setUsername(data.getString("username"));

                        if(link_type.equals("attempt_login")){
                            int flag = -1;
                            for (int i = 0; i < username_list.size(); i++){
                                if (username_list.get(i).equals(user.getUsername()))
                                    flag = i;
                            }
                            if(flag!=-1){
                                outputStream.writeUTF("rejected");
                            }
                            else {
                                //添加进用户名列表
                                username_list.add(data.getString("username"));
                                outputStream.writeUTF("accepted");
                            }
                            socket.close();
                        }
                        else if(link_type.equals("login")){
                            //添加进用户列表
                            user_list.add(user);
                            //信息显示区打印用户上线

                            try { // 插入文本
                                show_area.insertString(show_area.getLength(),"用户 " + user.getUsername() +
                                        " 在" + new Date() + " 登陆系统"+"\n",null);
                            } catch (BadLocationException be) {
                                be.printStackTrace();
                            }

                            //刷新在线人数
                            show_user.setText("人数有 " + username_list.size() + " 人\n");
                            //刷新在线用户
                            for (String s : username_list) {
                                show_user.append(s + "\n");
                            }

                            //封装信息对象
                            JSONObject online = new JSONObject();
                            //设置接收信息对象
                            online.put("user_list", username_list);

                            ArrayList<String> grps = new ArrayList<>();
                            for(String grpname:groupname_list){
                                int flag = 0;
                                String[] temp = grpname.split("#");
                                for(String grp:temp){
                                    if(grp.equals(user.getUsername())){
                                        flag = 1;
                                        break;
                                    }
                                }
                                if(flag == 1){
                                    grps.add(grpname);
                                }
                            }
                            //设置信息内容
                            String time = Utils.getTime();
                            online.put("msg",time + " " + user.getUsername() + "  上线了");
                            online.put("link_type", "chat");
                            online.put("channel_type","private");
                            online.put("channel","world");
                            //依次遍历，将信息广播给所有在线用户
//                            System.out.println(user_list.size());
                            for (User value : user_list) {
                                //获取输出流
                                outputStream = new DataOutputStream(value.getSocket().getOutputStream());
                                //给所有用户输出上线信息
                                if(value.getUsername().equals(user.getUsername())){
                                    JSONObject temp = online;
                                    temp.put("group_list", grps);
                                    outputStream.writeUTF(temp.toString());
                                }
                                else {
                                    outputStream.writeUTF(online.toString());
                                }
                            }

                            //开启新线程，持续接收该socket信息
                            new Thread(new Read(socket)).start();
                        }
                    }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    //线程代码
    class Read implements Runnable {
        //存放全局变量socket
        private final Socket socket;
        private volatile boolean exit = false;
        //构造函数，初始化socket
        public Read(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //获取输入流
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                //持续接收信息
                while (!exit) {
                    //获取传递进来的信息
                    String json = inputStream.readUTF();
                    //封装成json格式
                    JSONObject data = JSONObject.fromObject(json);
                    String link_type = data.getString("link_type");
                    if(link_type.equals("logout")){
                        String usr = data.getString("username");
                        for(int i = 0 ; i < user_list.size() ; i++){
                            if(user_list.get(i).getUsername().equals(usr)){
                                logout(i);
                                exit = true;
                                break;
                            }
                        }
                    }
                    else{
                        String channel_type = data.getString("channel_type");
                        if(link_type.equals("chat")) {
                            if (channel_type.equals("private")) {
                                String channel = data.getString("channel");

                                if (channel.equals("world")) {
                                    //构建信息内容
                                    String msg = data.getString("username") + "  " + data.getString("time") + "\n"
                                            + data.getString("msg");
                                    //添加到服务器显示

                                    try { // 插入文本
                                        show_area.insertString(show_area.getLength(),"[" +
                                                channel + "] " + msg +"\n",null);
                                    } catch (BadLocationException be) {
                                        be.printStackTrace();
                                    }

                                    //依次发给所有在线用户
                                    for (int i = 0; i < user_list.size(); ) {
                                        String target = "world";
                                        send_msg(i, msg, "private", target);
                                        i++;
                                    }
                                } else {
                                    //私发处理
                                    for (int i = 0; i < user_list.size(); i++) {
                                        //找到私发对象
                                        if (user_list.get(i).getUsername().equals(channel)) {
                                            //构建私发信息内容
                                            String msg = data.getString("username") + "  " + data.getString("time") + "\n" + data.getString("msg");
                                            String obj = data.getString("username");
                                            //用该方法指定对象发送信息
                                            send_msg(i, msg, "private", obj);

                                            //将发送成功反馈给原用户
                                            for (int j = 0; j < user_list.size(); j++) {
                                                //找到发信息用户
                                                if (user_list.get(j).getUsername().equals(data.getString("username"))) {
                                                    //构建反馈信息内容
                                                    msg = data.getString("time") + "\n" + data.getString("msg");
                                                    obj = user_list.get(i).getUsername();
                                                    //用该方法指定对象发送信息
                                                    send_msg(j, msg, "private", obj);
                                                    break;
                                                }
                                            }
                                            //将该操作打印到服务器监视窗

                                            try { // 插入文本
                                                show_area.insertString(show_area.getLength(),"[" +
                                                        channel + "] " + msg +"\n",null);
                                            } catch (BadLocationException be) {
                                                be.printStackTrace();
                                            }

                                            break;
                                        }
                                    }
                                }

                            } else {
//                                System.out.println("发群");
                                String channel = data.getString("channel");
                                String username = data.getString("username");
                                //构建信息内容
                                String msg = username + "  " + data.getString("time") + "\n"
                                        + data.getString("msg");
                                //添加到服务器显示

                                try { // 插入文本
                                    show_area.insertString(show_area.getLength(),"[" +
                                            channel + "] " + msg +"\n",null);
                                } catch (BadLocationException be) {
                                    be.printStackTrace();
                                }

                                String grp = data.getString("channel");
                                String[] users = grp.split("#");
                                for (String usr : users) {
                                    for (int i = 0; i < user_list.size(); i++) {
                                        //找到私发对象
                                        if (user_list.get(i).getUsername().equals(usr)) {
                                            //用该方法指定对象发送信息
                                            send_msg(i, msg, "group", channel);
                                        }
                                    }
                                }
                            }
                        }
                        else if(link_type.equals("addgroup")){
                            String username = data.getString("username");
                            String grp = data.getString("channel");
                            String[] users = grp.split("#");
                            int flag1 = -1;
                            for (int i = 0; i < groupname_list.size(); i++){
                                if (groupname_list.get(i).equals(grp))
                                    flag1 = i;
                            }
                            int flag2 = 0;
                            for (String usr:users){
//                                System.out.println(usr);
                                if(usr.equals(username))
                                    flag2 = 1;
                            }
                            if(flag1!=-1 || flag2==0){
                                outputStream.writeUTF("rejected");
                            }
                            else {
                                groupname_list.add(grp);

                                try { // 插入文本
                                    show_area.insertString(show_area.getLength(),"用户 " + username + " 在" + new Date() +
                                            " 创建用户组:"+grp+"\n",null);
                                } catch (BadLocationException be) {
                                    be.printStackTrace();
                                }

                                outputStream.writeUTF("accepted");

                                //封装信息对象
                                JSONObject online = new JSONObject();
                                //设置接收信息对象
                                online.put("group_list", groupname_list);
                                //设置信息内容
                                online.put("msg", Utils.getTime() + " 用户 " + username + " 将您加入了用户组 " + grp + "\n");
                                online.put("link_type", "chat");
                                online.put("channel_type","group");
                                online.put("channel",grp);
                                //依次遍历，将信息广播给用户组中所有在线用户
                                for (Object usr:users){
                                    for(User value : user_list){
                                        if(value.getUsername().equals(usr)){
                                            outputStream = new DataOutputStream(value.getSocket().getOutputStream());
                                            outputStream.writeUTF(online.toString());
                                            break;
                                        }
                                    }
                                }
                            }

                        }
                    }
                    }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //发送信息给指定用户的方法
        public void send_msg(int i, String msg, String tpe,String obj) {
            //构建对象
            JSONObject data = new JSONObject();
            //封装信息
            data.put("user_list", username_list);
            data.put("link_type", "chat");
            data.put("msg", msg);
            data.put("channel_type",tpe);
            data.put("channel",obj);
            //获取目标对象
            User user = user_list.get(i);
            try {
                //获取输出流
                outputStream = new DataOutputStream(user.getSocket().getOutputStream());
                //写信息
                outputStream.writeUTF(data.toString());
            } catch (IOException e) {
                logout(i);
            }

        }
        public void logout(int i){
            User out_user = user_list.get(i);
            //重复删除操作
            user_list.remove(i);
            serverFileThread.remove(i);
            serverPicThread.remove(i);
            username_list.remove(out_user.getUsername());
            //刷新在线人数
            show_user.setText("");
            show_user.setText("人数有 " + username_list.size() + " 人\n");

            try { // 插入文本
                show_area.insertString(show_area.getLength(),"用户 " + out_user.getUsername() +
                        " 在" + new Date() + " 退出系统"+"\n",null);
            } catch (BadLocationException be) {
                be.printStackTrace();
            }

            //刷新在线用户
            for (String s : username_list) {
                show_user.append(s + "\n");
            }

            //重新构建信息
            JSONObject out = new JSONObject();
            out.put("link_type","chat");
            out.put("channel_type","private");
            out.put("channel","world");
            out.put("user_list", username_list);
            String time = Utils.getTime();
            out.put("msg",time + " " + out_user.getUsername() + "  下线了\n");
            //将其下线通知广播给所有用户
            for (User value : user_list) {
                try {
                    outputStream = new DataOutputStream(value.getSocket().getOutputStream());
                    outputStream.writeUTF(out.toString());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}

