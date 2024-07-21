import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class Client extends JFrame{
    Socket socket;

    static JLabel chatTitle;
    JPanel thechatPanel;

    static String currentChannelType;
    static String currentPrivateObject;
    static String currentGroupObject;
    static HashMap<String,JTextPane> messagePrivate =new HashMap<>();
    static HashMap<String,JTextPane> messageGroup = new HashMap<>();
    static JTextPane message_area = new JTextPane();
    DataOutputStream outputStream;
    DataInputStream inputStream;
    String username;
    List<String> userList = new ArrayList<>();
    List<String> groupList = new ArrayList<>();

    boolean is_stop = false;

    JLabel onlineTitle;
    JLabel groupTitle;
    JList<Object> show_user_list = new JList<>();
    JList<Object> show_group_list = new JList<>();
    JPopupMenu popupMenu;
    List<Object> sel;
    ClientFileThread fileThread;
    ClientPicThread picThread;

    public Client(final String username) {

        this.username = username;
        currentChannelType = "private";
        currentPrivateObject = "world";
        messagePrivate.put(currentPrivateObject, new JTextPane());

//        System.out.println(Client.message_area);

        //布局
        setLayout(null);
        setSize(800, 550);
        setLocationRelativeTo(null);

        //窗口监听
        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("username",username);
                    data.put("link_type", "logout");
                    outputStream.writeUTF(data.toString());

                } catch (IOException ecp) {

                }
                System.exit(0);
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        setResizable(false);
        setTitle("ChatRoom   " + username);//窗口标题
        ImageIcon logo = new ImageIcon("media/logo.png");//logo
        setIconImage(logo.getImage());
        sendMessagePanel();//信息发送区域
        chatPanelFiled();//聊天区域
        channels();//频道

        setVisible(true);

        //招呼信息
        JSONObject data = new JSONObject();
        data.put("username", username);
        data.put("link_type", "login");

        //建立socket连接
        try {
            socket = new Socket(Utils.ServerIP, Utils.SeverThreadPort);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF(data.toString());

            //消息接收线程
            new Thread(new Read()).start();
            //文件传输线程
            fileThread = new ClientFileThread(username);
            fileThread.start();


            //图片传输线程
            picThread = new ClientPicThread(username);
            picThread.start();

            System.out.println("建立连接成功");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,"服务器无响应","提示",
                    JOptionPane.WARNING_MESSAGE);
        }



    }

    //信息发送区域
    private void sendMessagePanel() {
        final JPanel panel_south = new JPanel();
        panel_south.setLayout(new BorderLayout());
        panel_south.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 6));

        //消息编辑区域
        JTextArea send_area = new JTextArea();
        send_area.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 3));
        send_area.setBackground(new Color(240,240,240));
        panel_south.add(send_area, BorderLayout.CENTER);

        //发送按钮
        JButton send_btn = new JButton("发送");
        send_btn.setFont(new Font(null, Font.PLAIN, 13));
        panel_south.add(send_btn, BorderLayout.EAST);

        //功能按钮区域
        JPanel functionButtonPanel=new JPanel(new GridLayout(3,1,5,5));
        //发送图片
        JButton pictureButton=new JButton("图片");
        pictureButton.setFont(new Font(null, Font.PLAIN, 13));
        //发送文件
        JButton fileButton=new JButton("文件");
        fileButton.setFont(new Font(null, Font.PLAIN, 13));

        functionButtonPanel.add(pictureButton);
        functionButtonPanel.add(fileButton);
        functionButtonPanel.setBackground(new Color(255,255,255));

        panel_south.add(functionButtonPanel,BorderLayout.WEST);

        panel_south.setBackground(new Color(255, 255, 255));
        panel_south.setBounds(150,360,630,140);
        add(panel_south);


        //"发送图片"按钮监视
        pictureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPicOpenDialog();
            }
        });

        //"发送文件"按钮监视
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFileOpenDialog();
            }
        });

        //"发送"按钮监视
        send_btn.addActionListener(e -> {
            try {
                if (is_stop) {
                    JOptionPane.showMessageDialog(null,"您已被管理员强制下线","提示",
                            JOptionPane.WARNING_MESSAGE);
                    System.exit(0);
                } else {
                    String time = Utils.getTime();

                    String msg = send_area.getText().trim();

                    if (!msg.equals("")) {
                        JSONObject data = new JSONObject();
                        data.put("username", username);
                        data.put("msg", msg);
                        data.put("time", time);
                        data.put("link_type","chat");
                        data.put("channel_type",currentChannelType);
                        if(currentChannelType.equals("private"))
                        data.put("channel", currentPrivateObject);
                        else data.put("channel",currentGroupObject);
//                        System.out.println(data.getString("channel"));
                        outputStream.writeUTF(data.toString());
                    }
                }
                send_area.setText("");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

    }
    private void showFileOpenDialog() {
        // 创建一个默认的文件选择器
        JFileChooser fileChooser = new JFileChooser();

        // 默认打开的文件夹设置为本地桌面
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        fileChooser.setCurrentDirectory(desktopDir);

        // 设置默认使用的文件过滤器（FileNameExtensionFilter 的第一个参数是描述, 后面是需要过滤的文件扩展名 可变参数）
//        fileChooser.setFileFilter(new FileNameExtensionFilter("图像文件", "jpg", "jpeg", "png", "gif"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt"));

        // 打开文件选择框（线程将被堵塞，直到选择框被关闭）
        int result = fileChooser.showOpenDialog(new JPanel());  // 对话框将会尽量显示在靠近 parent 的中心
        // 点击确定
        if (result == JFileChooser.APPROVE_OPTION) {
            // 获取路径
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (currentChannelType.equals("private"))
                ClientFileThread.outFileToServer(path, currentChannelType, currentPrivateObject);
            else ClientFileThread.outFileToServer(path, currentChannelType, currentGroupObject);

        }
    }

    private void showPicOpenDialog() {
        // 创建一个默认的文件选择器
        JFileChooser fileChooser = new JFileChooser();

        // 默认打开的文件夹设置为本地桌面
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        fileChooser.setCurrentDirectory(desktopDir);

        // 设置默认使用的文件过滤器（FileNameExtensionFilter 的第一个参数是描述, 后面是需要过滤的文件扩展名 可变参数）
        fileChooser.setFileFilter(new FileNameExtensionFilter("图像文件", "jpg", "jpeg", "png"));
//        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt"));

        // 打开文件选择框（线程将被堵塞，直到选择框被关闭）
        int result = fileChooser.showOpenDialog(new JPanel());  // 对话框将会尽量显示在靠近 parent 的中心
        // 点击确定
        if (result == JFileChooser.APPROVE_OPTION) {
            // 获取路径
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (currentChannelType.equals("private"))
                ClientPicThread.outPicToServer(path, currentChannelType, currentPrivateObject);
            else ClientPicThread.outPicToServer(path, currentChannelType, currentGroupObject);
        }
    }


    //聊天区域
    private void chatPanelFiled() {
        //容器
        thechatPanel = new JPanel();
        thechatPanel.setLayout(new BorderLayout());

        //顶部标题组件
        chatTitle=new JLabel("world",SwingConstants.CENTER);
        chatTitle.setFont(new Font(null, Font.PLAIN, 15));
        thechatPanel.add(chatTitle,BorderLayout.NORTH);

        //中部可滚动消息列表组件
        message_area.setEditable(false);
        JScrollPane chatScrollPanel = new JScrollPane(message_area,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 6));
        thechatPanel.add(chatScrollPanel,BorderLayout.CENTER);

        //容器设置
        thechatPanel.setBackground(new Color(125, 186, 255));
        thechatPanel.setBounds(150,5,645,350);
        add(thechatPanel);
    }

    //频道
    private void channels(){
        JPanel channel = new JPanel(new BorderLayout());

        //世界
        JButton worldChatButton=new JButton("世界频道");
        worldChatButton.setFont(new Font(null, Font.PLAIN, 13));
        //"世界"按钮监视
        worldChatButton.addActionListener(e -> {
//            System.out.println("switch to world");
           currentChannelType = "private";
            currentPrivateObject ="world";
            chatTitle.setText("world");
            thechatPanel.setBackground(new Color(125, 186, 255));
            JTextPane temp = messagePrivate.get(currentPrivateObject);
//            System.out.println(temp.getText());
            message_area.setText(temp.getText());
        });
        channel.add(worldChatButton,BorderLayout.NORTH);

        //在线用户列表
        channel.add(showOnlineUser(),BorderLayout.CENTER);

        //组
        channel.add(showGroup(),BorderLayout.SOUTH);

        channel.setBounds(5,5,140,450);
        add(channel);
    }

    //在线用户列表
    private JPanel showOnlineUser() {

        //容器
        JPanel listPanel = new JPanel(new BorderLayout());

        //"顶部标题"组件
        onlineTitle=new JLabel("在线用户",SwingConstants.CENTER);
        onlineTitle.setFont(new Font(null, Font.PLAIN, 15));
        onlineTitle.setBounds(5,5,140,5);
        listPanel.add(onlineTitle,BorderLayout.NORTH);//添加"顶部标题"到容器

        //"用户列表"组件
        getOnlineUser();//"用户列表"初始化

        //“用户列表”内部组件:"右键选中用户功能表"
        JMenuItem[] menuItems=new JMenuItem[1];
        menuItems[0]=new JMenuItem("创建用户组");

        popupMenu =new JPopupMenu();
        for(int i = 0; i< menuItems.length; i++){
            //设置右键菜单字体
            menuItems[i].setFont(new Font(null,Font.BOLD,13));
            menuItems[i].addActionListener(e -> {
            });
            popupMenu.add(menuItems[i]);
        }
        show_user_list.add(popupMenu);

        //"用户列表"点击监视
        show_user_list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(show_user_list.getSelectedIndex()!=-1){
                    //左键双击
                    if(e.getButton()==1 && e.getClickCount()==2){

//                        System.out.println("双击了"+userList.get(show_user_list.getSelectedIndex()));
                        if (userList.get(show_user_list.getSelectedIndex()).equals(username)){
                            System.out.println("无法给自己发消息");
                        }
                        else{
                            StartPrivateChat(userList.get(show_user_list.getSelectedIndex()));//私聊
                        }
                    }
                    //右键单击
                    if(e.getButton()==3){
                        sel = show_user_list.getSelectedValuesList();
                        if(sel.size()>1){
                            System.out.println("选中了多个用户");
                            popupMenu.show(show_user_list,e.getX(),e.getY());
                        }
                    }
                }
            }
        });
        //"创建用户组"点击监视
        menuItems[0].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HashSet<String> group = new HashSet<>();
                for(Object o:sel)
                    group.add(o.toString());
                addGroup(group);
            }
        });

        //添加"用户列表"到容器
        show_user_list.setFont(new Font(null,Font.BOLD,13));
        listPanel.add(show_user_list,BorderLayout.CENTER);

        //其他容器配置
        listPanel.setBounds(5,5,140,200);
        listPanel.setBackground(new Color(255, 160, 125));

        return listPanel;
    }
    static public void StartPrivateChat(String chatPeople) {
//        System.out.println("StartPrivateChat");
        currentChannelType = "private";
        currentPrivateObject = chatPeople;
//        System.out.println(currentPrivateObject);
        chatTitle.setText(chatPeople);
        JTextPane temp = messagePrivate.get(currentPrivateObject);
        if(temp == null){
            temp = new JTextPane();
            messagePrivate.put(currentPrivateObject, temp);
        }
        message_area.setText(temp.getText());
    }
    private void getOnlineUser() {
        DefaultListModel DModel = new DefaultListModel();        //创建model
        for (int i = 0; i < userList.size(); i++) {
            DModel.addElement(userList.get(i));
        }
        show_user_list.setModel(DModel);
    }

    //组
    private JPanel showGroup(){
        //容器
        JPanel listPanel = new JPanel(new BorderLayout());

        //"顶部标题"组件
        groupTitle=new JLabel("用户组",SwingConstants.CENTER);
        groupTitle.setFont(new Font(null, Font.PLAIN, 15));
        listPanel.add(groupTitle,BorderLayout.NORTH);//添加"顶部标题"到容器

        //"用户组"组件
        getGroup();//"用户组"初始化

        //"用户组列表"点击监视
        show_group_list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(show_group_list.getSelectedIndex()!=-1){
                    //左键双击
                    if(e.getButton()==1 && e.getClickCount()==2){
                            StartGroupChat(groupList.get(show_group_list.getSelectedIndex()));//私聊
                        }
                    }
                }
        });

        //添加"用户组列表"到容器
        show_group_list.setFont(new Font(null,Font.BOLD,13));
        listPanel.add(show_group_list,BorderLayout.CENTER);

        //其他容器配置
        listPanel.setBounds(5,5,140,400);
        listPanel.setBackground(new Color(255, 160, 125));

        return listPanel;
    }
    static public void StartGroupChat(String chatPeople){
        currentChannelType = "group";
        currentGroupObject = chatPeople;
        chatTitle.setText(chatPeople);
        JTextPane temp = messageGroup.get(currentGroupObject);
        if(temp == null){
            temp = new JTextPane();
            messageGroup.put(currentGroupObject, temp);
        }
        message_area.setText(temp.getText());
    }
    private void getGroup(){
        DefaultListModel DModel = new DefaultListModel();        //创建model
        for (int i = 0; i < groupList.size(); i++) {
            DModel.addElement(groupList.get(i));
        }
        show_group_list.setModel(DModel);
    }
    private void addGroup(HashSet<String>grp){
        try {
            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("link_type","addgroup");
            data.put("channel_type","group");
            data.put("channel",set2string(grp));

            outputStream.writeUTF(data.toString());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,"服务器无响应","提示",
                    JOptionPane.WARNING_MESSAGE);
        }

    }
    private String set2string(HashSet<String> q){
//        System.out.println(q);
        String res="";
        for(String s:q) {
            res = res.concat(s.toString()).concat("#");
        }
        return res;
    }
    public class Read implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String json = inputStream.readUTF();

                    if (json.equals("accepted")) {
//                        System.out.println("用户组有效");
                        JOptionPane.showMessageDialog(null, "用户组已创建", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (json.equals("rejected")) {
                        JOptionPane.showMessageDialog(null, "未包含本用户或用户组重复", "提示",
                                JOptionPane.WARNING_MESSAGE);
                    }
                    else {
                        JSONObject data = JSONObject.fromObject(json);
                        String link_type = data.getString("link_type");

//                        System.out.println(link_type);

                        if (link_type.equals("kick") && data.getString("username").equals(username)) {
//                            System.out.println(data.getString("username"));
//                            System.out.println(username);
                            is_stop = true;
                            JOptionPane.showMessageDialog(null, "你已经被踢出群聊", "提示",
                                    JOptionPane.WARNING_MESSAGE);
                            System.exit(0);
                        }

                        String channel_type = data.getString("channel_type");
                        String msg = data.getString("msg");
                        if(data.get("user_list") != null){
                            JSONArray jsonArray = data.getJSONArray("user_list");
                            //在线总人数
                            onlineTitle.setText("在线用户(" + jsonArray.size() + ")");
                            //获取所有用户
                            userList.clear();
                            for (Object o : jsonArray) {
                                userList.add(o.toString());
                            }
                            //刷新在线用户
                            getOnlineUser();
                        }

                        if (data.get("group_list") != null) {
                            JSONArray groups = data.getJSONArray("group_list");
                            //加入的用户组数
                            groupTitle.setText("用户组(" + groups.size() + ")");
                            //获取用户组
                            groupList.clear();
                            for (Object o : groups) {
                                groupList.add(o.toString());
                            }
                            getGroup();
                        }
                        if (channel_type.equals("private")) {

                            String obj = data.getString("channel");
//                        System.out.println(pri);
                            StartPrivateChat(obj);
                            JTextPane temp = messagePrivate.get(currentPrivateObject);
                            StyledDocument text = temp.getStyledDocument();
                            try { // 插入文本
                                text.insertString(text.getLength(),msg + "\n",null);
                            } catch (BadLocationException be) {
                                be.printStackTrace();
                            }
//                        System.out.println("内容："+msg+"添加到"+currentPrivateObject);
                            message_area.setText(temp.getText());
                            messagePrivate.replace(currentPrivateObject, temp);
                        } else {
                            String grp = data.getString("channel");
                            StartGroupChat(grp);
                            JTextPane temp = messageGroup.get(currentGroupObject);
                            StyledDocument text = temp.getStyledDocument();
                            try { // 插入文本
                                text.insertString(text.getLength(),msg + "\n",null);
                            } catch (BadLocationException be) {
                                be.printStackTrace();
                            }
                            message_area.setText(temp.getText());
                            messageGroup.replace(currentGroupObject, temp);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


