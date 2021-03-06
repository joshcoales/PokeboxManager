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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import uk.org.spangle.controller.Controller;
import uk.org.spangle.data.*;
import uk.org.spangle.model.Configuration;

import java.util.ArrayList;
import java.util.List;

public class SideBar {
    private Pane sideBarPane;
    private ChoiceBox<UserGame> gameDropdown;
    private ChoiceBox<UserBox> boxDropdown;
    private Canvas boxCanvas;
    private UserGame currentGame = null;
    private Session session;
    private Configuration conf;
    private Controller controller;

    SideBar(Pane sideBarPane, Session session, Configuration conf, final Controller controller) {
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
        boxCanvas = createBoxCanvas();

        // Buttons
        Button gameButton = new Button("Modify game list");
        Button unknownButton = new Button("Unknown values");
        unknownButton.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                controller.viewUnknown();
            }
        });
        Button confButton = new Button("Configuration");
        confButton.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                controller.viewConfig();
            }
        });

        // Add everything to the sidebar
        VBox sideBarVBox = new VBox();
        sideBarVBox.getChildren().addAll(titleText,gameDropdown,boxText,boxButtons,boxCanvas,gameButton,unknownButton,confButton);

        sideBarPane.getChildren().add(sideBarVBox);
    }

    private ChoiceBox<UserGame> createGameDropdown() {

        // Now lets pull games list from the database
        @SuppressWarnings("unchecked")
        final List<UserGame> gameList = (List<UserGame>) session.createCriteria(UserGame.class).addOrder(Order.asc("ordinal")).list();

        ChoiceBox<UserGame> gameDropdown = new ChoiceBox<>();
        gameDropdown.setItems(FXCollections.observableArrayList(gameList));
        gameDropdown.setValue(currentGame);
        gameDropdown.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<UserGame>() {
            @Override
            public void changed(ObservableValue<? extends UserGame> observableValue, UserGame old_value, UserGame new_value) {
                controller.updateGame(new_value);
            }
        });
        return gameDropdown;
    }

    private ChoiceBox<UserBox> createBoxDropdown() {
        boxDropdown = new ChoiceBox<>();
        updateBoxDropdown();
        boxDropdown.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<UserBox>() {
            @Override
            public void changed(ObservableValue<? extends UserBox> observableValue, UserBox old_value, UserBox new_value) {
                if(new_value != null) controller.updateBox(currentGame,new_value);
            }
        });
        return boxDropdown;
    }

    public void updateBoxDropdown() {
        List<UserBox> listBoxes = currentGame.getUserBoxes();
        boxDropdown.setItems(FXCollections.observableArrayList(listBoxes));
        boxDropdown.setValue(null);
        UserBox currentBox = currentGame.getCurrentBox();
        if(currentBox != null) {
            boxDropdown.setValue(currentBox);
        }
    }

    private Canvas createBoxCanvas() {
        boxCanvas = new Canvas();
        boxCanvas.setWidth(30*6);
        boxCanvas.setHeight(30*5);
        updateBoxCanvas();
        boxCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED,
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent t) {
                        controller.clickCanvas(t);
                    }
                });
        return boxCanvas;
    }

    public void updateBoxCanvas() {
        GraphicsContext graphicsContext = boxCanvas.getGraphicsContext2D();

        graphicsContext.clearRect(0,0,boxCanvas.getWidth(),boxCanvas.getHeight());
        // Box sprite sheet
        Image image = conf.getImagePokemonIcons();
        Image eggImage = conf.getImageEgg();

        // Get list of user pokemon:
        List<UserPokemon> pokemonList = new ArrayList<>();
        if(currentGame.getCurrentBox() != null) {
            UserBox currentBox = currentGame.getCurrentBox();
            graphicsContext.setFill(Color.PALEGREEN);
            graphicsContext.fillRect(0,0,currentBox.getColumns()*30,currentBox.getSize()/currentBox.getColumns()*30);
            pokemonList = currentBox.getUserPokemons();
        }
        for(UserPokemon userPokemon : pokemonList) {
            int x_coord = userPokemon.getSpriteX();
            int y_coord = userPokemon.getSpriteY();
            int box_x = (((userPokemon.getPosition()-1) % currentGame.getCurrentBox().getColumns()) *30) -5;
            int box_y = ((userPokemon.getPosition()-1) / currentGame.getCurrentBox().getColumns()) *30;
            UserPokemonEgg upe = userPokemon.getUserPokemonEgg();
            if(upe != null && upe.getIsEgg() && !conf.getHideEggs()) {
                graphicsContext.drawImage(eggImage,0,0,40,30,box_x,box_y,40,30);
                continue;
            }
            graphicsContext.drawImage(image,x_coord,y_coord,40,30,box_x,box_y,40,30);
        }
    }

    public void setGame(UserGame userGame) {
        // Update box dropdown
        this.currentGame = userGame;
        updateBoxDropdown();
    }
}
