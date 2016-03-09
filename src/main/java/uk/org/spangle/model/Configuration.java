package uk.org.spangle.model;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import uk.org.spangle.data.UserConfig;
import uk.org.spangle.data.UserGame;

import java.util.List;

public class Configuration {
    Session session;

    public Configuration(Session session) {
        this.session = session;
    }

    private UserConfig getUserConfig(String key) {
        session.beginTransaction();
        List list = session.createCriteria(UserConfig.class).add(Restrictions.eq("key",key)).list();

        UserConfig userConfig = null;
        for (Object obj : list) {
            userConfig = (UserConfig) obj;
        }

        session.getTransaction().commit();
        return userConfig;
    }

    private String getUserConfigValue(String key) {
        UserConfig userConfig = this.getUserConfig(key);
        if(userConfig == null) return null;
        return userConfig.getValue();
    }

    public Integer getCurrentGameId() {
        String gameId = this.getUserConfigValue("current_game");
        if(gameId == null) return null;
        return Integer.parseInt(gameId);
    }

    public void setCurrentGameId(int currentGameId) {
        UserConfig currentGameConfig = this.getUserConfig("current_game");
        if(currentGameConfig == null) {
            session.beginTransaction();
            UserConfig userConfig = new UserConfig();
            userConfig.setKey("current_game");
            userConfig.setValue(String.valueOf(currentGameId));
            session.getTransaction().commit();
            return;
        }

        session.beginTransaction();
        currentGameConfig.setValue(String.valueOf(currentGameId));
        session.getTransaction().commit();
    }

    public void setCurrentGame(UserGame currentGame) {
        int currentGameId = currentGame.getId();
        this.setCurrentGameId(currentGameId);
    }

    public UserGame getCurrentGame() {
        Integer currentGameId = this.getCurrentGameId();
        if(currentGameId == null) return null;

        session.beginTransaction();
        List list = session.createCriteria(UserGame.class).add(Restrictions.eq("id",currentGameId)).list();

        UserGame userGame = null;
        for (Object obj : list) {
            userGame = (UserGame) obj;
        }

        session.getTransaction().commit();
        return userGame;
    }

    public Integer getCurrentBoxId() {
        String boxId = this.getUserConfigValue("current_box");
        if(boxId != null) {
            return Integer.parseInt(boxId);
        }
        return null;
    }

    public void setCurrentBoxId(int currentBoxId) {
        return;
        //TODO:
    }
}
