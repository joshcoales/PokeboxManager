package uk.org.spangle.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import uk.org.spangle.controller.Controller;
import uk.org.spangle.data.*;
import uk.org.spangle.model.Configuration;

import java.util.ArrayList;
import java.util.List;

public class SideBar {
    Pane sideBarPane;
    ChoiceBox<String> gameDropdown;
    ChoiceBox<String> boxDropdown;
    UserGame currentGame = null;
    Session session;
    Configuration conf;
    Controller controller;

    public SideBar(Pane sideBarPane, Session session, Configuration conf, final Controller controller) {
        this.sideBarPane = sideBarPane;
        this.session = session;
        this.conf = conf;
        this.controller = controller;

        // Get current game
        currentGame = conf.getCurrentGame();

        // Game selector and titles
        Text titleText = new Text("Game");
        gameDropdown = createGameDropdown();

        // Boxes selector and buttons
        Text boxText = new Text("Box");
        Button boxLeft = new Button("<");
        boxLeft.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                controller.prevBox(currentGame);
            }
        });
        boxDropdown = createBoxDropdown();
        updateBoxDropdown();
        Button boxRight = new Button(">");
        boxRight.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                controller.nextBox(currentGame);
            }
        });
        HBox boxButtons = new HBox();
        boxButtons.getChildren().addAll(boxLeft,boxDropdown,boxRight);

        // Box canvas
        Canvas boxCanvas = null;
        try {
            boxCanvas = createBoxCanvas();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add everything to the sidebar
        VBox sideBarVBox = new VBox();
        sideBarVBox.getChildren().addAll(titleText,gameDropdown,boxText,boxButtons,boxCanvas);

        sideBarPane.getChildren().add(sideBarVBox);
    }

    public ChoiceBox<String> createGameDropdown() {

        // now lets pull events from the database and list them
        session.beginTransaction();
        @SuppressWarnings("unchecked")
        final List<UserGame> gameList = (List<UserGame>) session.createCriteria(UserGame.class).addOrder(Order.asc("ordinal")).list();
        List<String> nameList = new ArrayList<>();
        for (Object obj : gameList) {
            UserGame event = (UserGame) obj;
            nameList.add(event.getName());
            if(currentGame == null) currentGame = event;
        }
        session.getTransaction().commit();

        ChoiceBox<String> gameDropdown = new ChoiceBox<>();
        gameDropdown.setItems(FXCollections.observableArrayList(nameList));
        gameDropdown.setValue(currentGame.getName());
        gameDropdown.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number old_value, Number new_value) {
                UserGame selectedGame = gameList.get(new_value.intValue());
                controller.updateGame(selectedGame);
            }
        });
        return gameDropdown;
    }

    public ChoiceBox<String> createBoxDropdown() {
        boxDropdown = new ChoiceBox<>();
        boxDropdown.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number old_value, Number new_value) {
                if(new_value.intValue() == -1) return;
                controller.updateBox(currentGame,new_value.intValue());
            }
        });
        return boxDropdown;
    }

    public void updateBoxDropdown() {
        List<UserBox> listBoxes = currentGame.getUserBoxes();
        List<String> listNames = new ArrayList<>();
        System.out.println(listBoxes);
        for(UserBox userBox : listBoxes) {
            listNames.add(userBox.getName());
        }
        boxDropdown.setItems(FXCollections.observableArrayList(listNames));
        boxDropdown.setValue(null);
        UserBox currentBox = currentGame.getCurrentBox();
        if(currentBox != null) {
            boxDropdown.setValue(currentBox.getName());
        }
    }

    public Canvas createBoxCanvas() throws Exception {
        Canvas boxCanvas = new Canvas();
        boxCanvas.setWidth(40*6);
        boxCanvas.setHeight(30*5);
        GraphicsContext graphicsContext = boxCanvas.getGraphicsContext2D();

        // Box sprite sheet
        Image image = new Image(getClass().getResourceAsStream("/box_sprites.png"));

        // Get list of user pokemon:
        List<UserPokemon> pokemonList = currentGame.getCurrentBox().getUserPokemons();
        for(UserPokemon userPokemon : pokemonList) {
            Pokemon pokemon = userPokemon.getPokemon();
            PokemonForm form = pokemon.getPokemonForms().get(0);
            int x_coord = form.getSpriteMaleX();
            int y_coord = form.getSpriteMaleY();
            int box_x = ((userPokemon.getPosition()-1) % currentGame.getCurrentBox().getColumns()) *40;
            int box_y = ((userPokemon.getPosition()-1) / currentGame.getCurrentBox().getColumns()) *30;
            graphicsContext.drawImage(image,x_coord,y_coord,40,30,box_x,box_y,40,30);
        }

        return boxCanvas;
    }

    public void setGame(UserGame userGame) {
        // Update box dropdown
        this.currentGame = userGame;
        updateBoxDropdown();
    }

    public void setBox(UserBox userBox) {
        // Update box canvas and such
    }
}
