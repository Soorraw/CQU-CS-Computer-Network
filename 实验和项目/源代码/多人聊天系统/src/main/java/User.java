import java.net.Socket;

//用户类
public class User {
    private String username;    //用户名

    private Socket socket;      //socket

    public String getUsername() {   //获取用户名
        return username;
    }

    public void setUsername(String username) {      //设置用户名
        this.username = username;
    }

    public Socket getSocket() {     //获取socket
        return socket;
    }

    public void setSocket(Socket socket) {      //设置socket
        this.socket = socket;
    }
}



