import java.nio.channels.SelectionKey;

/**
 * Created by weicheng on 2015/7/19.
 */
public class UserToUser {
    private String user;

    public String getOtherUser() {
        return otherUser;
    }

    public void setOtherUser(String otherUser) {
        this.otherUser = otherUser;
    }

    private String otherUser;
    public UserToUser(String user,String other){
        this.user = user;
        this.otherUser = other;
    }
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


}
