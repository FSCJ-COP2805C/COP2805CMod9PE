// HappyBirthdayApp.java
// D. Singletary
// 1/29/23
// wish multiple users a happy birthday

// D. Singletary
// 2/26/23
// Added Stream and localization code

// D. Singletary
// 3/7/23
// Changed to thread-safe queue
// Moved buildCard to BirthdayCard class
// Instantiate the BirthdayCardProcessor object
// added test data for multi-threading tests

package edu.fscj.cop2805c.birthday;

import edu.fscj.cop2805c.dispatch.Dispatcher;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

// main mpplication class
public class HappyBirthdayApp implements BirthdayGreeter, Dispatcher<BirthdayCard>  {

    private static final String USER_FILE = "user.dat";

    private ArrayList<User> birthdays = new ArrayList<>();
    // Use a thread-safe Queue<LinkedList> to act as message queue for the dispatcher
    ConcurrentLinkedQueue safeQueue = new ConcurrentLinkedQueue(
           new LinkedList<BirthdayCard>()
    );

    private Stream<BirthdayCard> stream = safeQueue.stream();
    //ObjectOutputStream userData = null;

    public HappyBirthdayApp() { }

    // dispatch the card using the dispatcher
    public void dispatch(BirthdayCard bc) {
        this.safeQueue.add(bc);
    }

    // send the card
    public void sendCard(BirthdayCard bc) {
        // dispatch the card
        Dispatcher<BirthdayCard> d = (c)-> {
            this.safeQueue.add(c);
        };
        d.dispatch(bc);
    }

    // show prompt msg with no newline
    public static void prompt(String msg) {
        System.out.print(msg + ": ");
    }

    public void generateCards() {

        for (User u : birthdays) {
            System.out.println(u.getName());
            // see if today is their birthday
            // if not, show sorry message
            if (!BirthdayCard.isBirthday(u))
                System.out.println("Sorry, today is not their birthday.");
                // otherwise build the card
            else {
                String msg = "";
                try {
                    // load the property and create the localized greeting
                    ResourceBundle res = ResourceBundle.getBundle(
                            "edu.fscj.cop2805c.birthday.Birthday", u.getLocale());
                    String happyBirthday = res.getString("HappyBirthday");

                    // format and display the date
                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                    formatter =
                            formatter.localizedBy(u.getLocale());
                    msg = u.getBirthday().format(formatter) + "\n";

                    // add the localized greeting
                    msg += happyBirthday + " " + u.getName() + "\n" +
                            BirthdayCard.WISHES;
                } catch (java.util.MissingResourceException e) {
                    System.err.println(e);
                    msg = "Happy Birthday, " + u.getName() + "\n" +
                            BirthdayCard.WISHES;
                }
                BirthdayCard bc = new BirthdayCard(u, msg);
                sendCard(bc);
            }
        }
        birthdays.clear(); // clear the list
    }

    // add multiple birthdays
    public void addBirthdays(User... users) {
        for (User u : users) {
            birthdays.add(u);
        }
    }

    // write user ArrayList to save file
    public void writeUsers(ArrayList<User> ul) {
        try (ObjectOutputStream userData =  new ObjectOutputStream(
                new FileOutputStream(USER_FILE));) {
                userData.writeObject(ul);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // read saved user ArrayList
    public ArrayList<User> readUsers() {
        ArrayList<User> list = new ArrayList();

        try (ObjectInputStream userData =
                     new ObjectInputStream(
                             new FileInputStream(USER_FILE));) {
            list = (ArrayList<User>) (userData.readObject());
            for (User u : list)
                System.out.println("readUsers: read " + u);
        } catch (FileNotFoundException e) {
            // not  a problem if nothing was saved
            System.err.println("readUsers: no input file");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
           e.printStackTrace();
        }
        return list;    
    }

    // main program
    public static void main(String[] args) {

        HappyBirthdayApp hba = new HappyBirthdayApp();

        // restore saved data
        ArrayList<User> userList = hba.readUsers();

        // start the processor thread
        BirthdayCardProcessor processor = new BirthdayCardProcessor(hba.safeQueue);

        // use current date for testing, adjust where necessary
        ZonedDateTime currentDate = ZonedDateTime.now();

        // if no users, generate some for testing
        if (userList.isEmpty()) {
            // negative test
            userList.add(new User("Dianne", "Romero", "Dianne.Romero@email.test",
                    new Locale("en"), currentDate.minusDays(1)));

            // positive tests
            // test with odd length full name and english locale
            userList.add(new User("Sally", "Ride", "Sally.Ride@email.test",
                    new Locale("en"), currentDate));

            // test french locale
            userList.add(new User("René", "Descartes", "René.Descartes@email.test",
                    new Locale("fr"), currentDate));

            // test with even length full name and german locale
            userList.add(new User("Johannes", "Brahms", "Johannes.Brahms@email.test",
                    new Locale("de"), currentDate));

            // test chinese locale
            userList.add(new User("Charles", "Kao", "Charles.Kao@email.test",
                    new Locale("zh"), currentDate));
        }
        else
            System.out.println("Users were read from data file");

        // convert ArrayList of users to array for addBirthdays method
        User[] userArray = new User[userList.size()];
        userArray = userList.toArray(userArray);
        hba.addBirthdays(userArray);
        hba.generateCards();

        // wait for a bit
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            System.out.println("sleep interrupted! " + ie);
        }

        processor.endProcessing();

        // generate (or regenerate) the user data file
        hba.writeUsers(userList);
    }
}