package uk.org.spangle.controller;

import javafx.scene.input.MouseEvent;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import uk.org.spangle.data.*;
import uk.org.spangle.model.Configuration;
import uk.org.spangle.view.App;
import uk.org.spangle.view.AutoCompleteTextField;

import java.util.List;

public class Controller {
    private Session session;
    private Configuration conf;
    private App app;

    public Controller(Session session, Configuration conf, App app) {
        this.session = session;
        this.conf = conf;
        this.app = app;
    }

    public void updateGame(UserGame value) {
        conf.setCurrentGame(value);
        app.getSideBar().setGame(value);
    }

    public void updateBox(UserGame currentGame, UserBox currentBox) {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            currentGame.setCurrentBox(currentBox);
            session.update(currentGame);
            tx.commit();
        } catch (Exception ex) {
            if(tx != null) tx.rollback();
            throw ex;
        }
        app.getSideBar().updateBoxCanvas();
    }

    public void prevBox(UserGame currentGame) {
        UserBox currentBox = currentGame.getCurrentBox();
        List<UserBox> listBoxes = currentGame.getUserBoxes();
        int index = listBoxes.indexOf(currentBox);
        // Remove 1 from index and wrap it around
        int newIndex = ((index-1) % listBoxes.size() + listBoxes.size()) % listBoxes.size();
        UserBox newBox = listBoxes.get(newIndex);
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            currentGame.setCurrentBox(newBox);
            session.update(currentGame);
            tx.commit();
        } catch (Exception ex) {
            if(tx != null) tx.rollback();
            throw ex;
        }
        app.getSideBar().updateBoxDropdown();
        app.getSideBar().updateBoxCanvas();
    }

    public void nextBox(UserGame currentGame) {
        UserBox currentBox = currentGame.getCurrentBox();
        List<UserBox> listBoxes = currentGame.getUserBoxes();
        int index = listBoxes.indexOf(currentBox);
        int newIndex = (index+1) % listBoxes.size();
        UserBox newBox = listBoxes.get(newIndex);
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            currentGame.setCurrentBox(newBox);
            session.update(currentGame);
            tx.commit();
        } catch (Exception ex) {
            if(tx != null) tx.rollback();
            throw ex;
        }
        app.getSideBar().updateBoxDropdown();
        app.getSideBar().updateBoxCanvas();
    }

    public void clickCanvas(MouseEvent t) {
        int x = (int) t.getX();
        int y = (int) t.getY();
        int box_x = x/30;
        int box_y = y/30;
        UserBox userBox = conf.getCurrentGame().getCurrentBox();
        int position = box_y*(userBox.getColumns()) + box_x + 1;
        List list = session.createCriteria(UserPokemon.class).add(Restrictions.eq("userBox",userBox)).add(Restrictions.eq("position",position)).list();
        if(list.size() == 0) {
            System.out.println("No pokemon here");
            clickCanvasEmpty(position);
        } else {
            UserPokemon userPokemon = (UserPokemon) list.get(0);
            clickCanvasPokemon(userPokemon);
        }

    }

    private void clickCanvasEmpty(int position) {
        UserBox userBox = conf.getCurrentGame().getCurrentBox();
        app.getInfoBox().addNewPokemon(userBox,position);
    }

    private void clickCanvasPokemon(UserPokemon userPokemon) {
        app.getInfoBox().displayPokemon(userPokemon);
    }

    public void addPokemon(AutoCompleteTextField speciesBox, UserBox userBox, int position) {
        Pokemon pokemon = speciesBox.getSelectedPokemon();
        if(pokemon == null) {
            String pokemonName = speciesBox.getText();
            pokemon = (Pokemon) session.createCriteria(Pokemon.class).add(Restrictions.eq("name",pokemonName)).uniqueResult();
            if(pokemon == null) {
                System.out.println("Invalid!");
                return;
            }
        }
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            UserPokemon userPokemon = new UserPokemon(userBox, position, pokemon);
            session.save(userPokemon);
            tx.commit();
            session.refresh(userBox);
            app.getSideBar().updateBoxCanvas();
            app.getInfoBox().displayPokemon(userPokemon);
        } catch (Exception ex) {
            if(tx != null) tx.rollback();
            throw ex;
        }
    }

    public void updatePokemonBall(UserPokemon userPokemon, PokeBall old_val, PokeBall new_val) {
        if(old_val == new_val) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonBall() != null) session.delete(userPokemon.getUserPokemonBall());
            if (new_val == null) {
                userPokemon.setUserPokemonBall(null);
            } else {
                UserPokemonBall upb = new UserPokemonBall(userPokemon, new_val);
                session.save(upb);
                userPokemon.setUserPokemonBall(upb);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonEgg(UserPokemon userPokemon, String old_val, String new_val) {
        if(old_val.equals(new_val)) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonEgg() != null) session.delete(userPokemon.getUserPokemonEgg());
            if (new_val.equals(UserPokemonEgg.UNKNOWN)) {
                userPokemon.setUserPokemonEgg(null);
            } else {
                UserPokemonEgg upe = new UserPokemonEgg(userPokemon, new_val.equals(UserPokemonEgg.IS_EGG));
                session.save(upe);
                userPokemon.setUserPokemonEgg(upe);
            }
            session.update(userPokemon);
            tx.commit();
            if (conf.getHideEggs()) return;
            if (new_val.equals(UserPokemonEgg.IS_EGG) != old_val.equals(UserPokemonEgg.IS_EGG))
                app.getSideBar().updateBoxCanvas();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonESV(UserPokemon userPokemon, String newVal) {
        Transaction tx = null;
        try {
            session.refresh(userPokemon);
            tx = session.beginTransaction();
            UserPokemonESV upe = userPokemon.getUserPokemonESV();
            if(newVal.length() == 0) {
                if(upe != null) session.delete(upe);
            } else {
                if (upe == null) {
                    upe = new UserPokemonESV(userPokemon, Integer.parseInt(newVal));
                    session.save(upe);
                } else {
                    upe.setESV(Integer.parseInt(newVal));
                    session.update(upe);
                }
            }
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonForm(UserPokemon userPokemon, PokemonForm old_val, PokemonForm new_val) {
        if(old_val == new_val) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonForm() != null) session.delete(userPokemon.getUserPokemonForm());
            if (new_val == null) {
                userPokemon.setUserPokemonForm(null);
            } else {
                UserPokemonForm upf = new UserPokemonForm(userPokemon, new_val);
                session.save(upf);
                userPokemon.setUserPokemonForm(upf);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
        app.getSideBar().updateBoxCanvas();
    }

    public void updatePokemonLanguage(UserPokemon userPokemon, Language old_val, Language new_val) {
        if(old_val == new_val) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonLanguage() != null) session.delete(userPokemon.getUserPokemonLanguage());
            if(new_val == null) {
                userPokemon.setUserPokemonLanguage(null);
            } else {
                UserPokemonLanguage userPokemonLanguage = new UserPokemonLanguage(userPokemon, new_val);
                session.save(userPokemonLanguage);
                userPokemon.setUserPokemonLanguage(userPokemonLanguage);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonNature(UserPokemon userPokemon, Nature old_val, Nature new_val) {
        if(old_val == new_val) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonNature() != null) session.delete(userPokemon.getUserPokemonNature());
            if(new_val == null) {
                userPokemon.setUserPokemonNature(null);
            } else {
                UserPokemonNature userPokemonNature = new UserPokemonNature(userPokemon, new_val);
                session.save(userPokemonNature);
                userPokemon.setUserPokemonNature(userPokemonNature);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonNickname(UserPokemon userPokemon, boolean noNick, String nickname) {
        Transaction tx = null;
        try {
            session.refresh(userPokemon);
            tx = session.beginTransaction();
            UserPokemonNickname userPokemonNickname = userPokemon.getUserPokemonNickname();
            if(userPokemonNickname != null) {
                if (!noNick && nickname.length() == 0) {
                    session.delete(userPokemonNickname);
                } else if(noNick) {
                    userPokemonNickname.setNickname(null);
                    session.update(userPokemonNickname);
                } else {
                    userPokemonNickname.setNickname(nickname);
                    session.update(userPokemonNickname);
                }
            } else {
                if(noNick) {
                    UserPokemonNickname upn = new UserPokemonNickname(userPokemon, null);
                    session.save(upn);
                    userPokemon.setUserPokemonNickname(upn);
                    session.update(userPokemon);
                } else if(nickname.length() > 0) {
                    UserPokemonNickname upn = new UserPokemonNickname(userPokemon, nickname);
                    session.save(upn);
                    userPokemon.setUserPokemonNickname(upn);
                    session.update(userPokemon);
                }
            }
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonPokerus(UserPokemon userPokemon, String old_val, String new_val) {
        if(old_val.equals(new_val)) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonPokerus() != null) session.delete(userPokemon.getUserPokemonPokerus());
            if (new_val.equals(UserPokemonPokerus.UNKNOWN)) {
                userPokemon.setUserPokemonPokerus(null);
            } else {
                UserPokemonPokerus upp = new UserPokemonPokerus(userPokemon, new_val.equals(UserPokemonPokerus.HAS_POKERUS));
                session.save(upp);
                userPokemon.setUserPokemonPokerus(upp);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonSex(UserPokemon userPokemon, String old_val, String new_val) {
        if(old_val.equals(new_val)) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonSex() != null) session.delete(userPokemon.getUserPokemonSex());
            if (new_val.equals(UserPokemonSex.UNKNOWN)) {
                userPokemon.setUserPokemonSex(null);
            } else {
                UserPokemonSex ups = new UserPokemonSex(userPokemon, new_val.equals(UserPokemonSex.MALE));
                session.save(ups);
                userPokemon.setUserPokemonSex(ups);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
        app.getSideBar().updateBoxCanvas();
    }

    public void updatePokemonShiny(UserPokemon userPokemon, String old_val, String new_val) {
        if(old_val.equals(new_val)) return;
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonShiny() != null) session.delete(userPokemon.getUserPokemonShiny());
            if (new_val.equals(UserPokemonShiny.UNKNOWN)) {
                userPokemon.setUserPokemonShiny(null);
            } else {
                UserPokemonShiny ups = new UserPokemonShiny(userPokemon, new_val.equals(UserPokemonShiny.IS_SHINY));
                session.save(ups);
                userPokemon.setUserPokemonShiny(ups);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
        app.getSideBar().updateBoxCanvas();
    }

    public void updatePokemonAbility(UserPokemon userPokemon, PokemonFormAbility oldVal, PokemonFormAbility newVal) {
        if(oldVal == newVal) return;
        if(oldVal != null && newVal != null) {
            if(oldVal.getAbilitySlot() == newVal.getAbilitySlot()) return;
        }
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            if(userPokemon.getUserPokemonAbilitySlot() != null) session.delete(userPokemon.getUserPokemonAbilitySlot());
            if(newVal == null) {
                userPokemon.setUserPokemonAbilitySlot(null);
            } else {
                UserPokemonAbilitySlot upas = new UserPokemonAbilitySlot(userPokemon, newVal.getAbilitySlot());
                session.save(upas);
                userPokemon.setUserPokemonAbilitySlot(upas);
            }
            session.update(userPokemon);
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void updatePokemonLevel(UserPokemon userPokemon, String newVal) {
        Transaction tx = null;
        try {
            session.refresh(userPokemon);
            tx = session.beginTransaction();
            UserPokemonLevel upl = userPokemon.getUserPokemonLevel();
            if(newVal.length() == 0) {
                if(upl != null) session.delete(upl);
            } else {
                if (upl == null) {
                    upl = new UserPokemonLevel(userPokemon, Integer.parseInt(newVal));
                    session.save(upl);
                } else {
                    upl.setLevel(Integer.parseInt(newVal));
                    session.update(upl);
                }
            }
            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            throw e;
        }
    }

    public void removePokemon(UserPokemon userPokemon) {
        UserBox userBox = userPokemon.getUserBox();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(userPokemon);
            tx.commit();
        } catch (Exception ex) {
            if(tx != null) tx.rollback();
            throw ex;
        }
        session.refresh(userBox);
        app.getSideBar().updateBoxCanvas();
        app.getInfoBox().blankInfoBox();
    }

    public void viewConfig() {
        app.getInfoBox().viewConfig();
    }

    public void updateConfigHideEggs(boolean newVal) {
        conf.setHideEggs(newVal);
        app.getSideBar().updateBoxCanvas();
    }

    public void viewUnknown() {
        app.getInfoBox().viewUnknown();
    }
}
