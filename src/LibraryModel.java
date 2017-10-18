/*
 * LibraryModel.java
 * Author:
 * Created on:
 */



import com.sun.org.apache.bcel.internal.generic.RETURN;

import javax.swing.*;
import java.io.Reader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LibraryModel {

    // For use in creating dialogs and making them modal
    private JFrame dialogParent;
    private Connection conn;
    private String databaseURL;

    private final String ERROR_RETRV_RECORD = "Error retriving record from DB";

    public LibraryModel(JFrame parent, String userid, String password) {
        dialogParent = parent;

        this.databaseURL = String.format("jdbc:postgresql://localhost:5432/%s_jdbc?user=%s&password=%s",
                userid, userid, password);
        try {
            this.conn = DriverManager.getConnection(this.databaseURL);

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Not connected to database!");
            System.exit(-1);
        }

        System.out.println("Successfully connected to db");
    }

    public String bookLookup(int isbn) {
        String item = "";
        try {

            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT isbn, title, edition_no, numofcop, numleft, surname " +
                    "FROM book NATURAL JOIN book_author " +
                    "NATURAL JOIN author " +
                    "WHERE isbn = ?");
            ps.setInt(1, isbn);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                isbn = rs.getInt("isbn");
                String title = rs.getString("title");
                int edition = rs.getInt("edition_no");
                int numCop = rs.getInt("numofcop");
                int copLeft = rs.getInt("numleft");
                String author = rs.getString("surname");
                item = String.format("\t%d: %s\n" +
                        "\tEdition: %d - Number of copies: %d - Copies left: %d\n" +
                        "\tAuthor: %s", isbn, title, edition, numCop, copLeft, author);
            }

            if (item != null)
                return "Book Lookup:\n" + item;

            closeStatements(ps, rs);
        }catch (Exception e) {
            return this.ERROR_RETRV_RECORD;
        }
        return "No Collection of book contianing: " + isbn;
    }

    private void closeStatements(PreparedStatement s, ResultSet r) throws SQLException {
        s.close();
        r.close();
    }

    public String showCatalogue() {
        String catalogue = "";
        try {
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT isbn, title, edition_no, numofcop, numleft, surname " +
                    "FROM book NATURAL JOIN book_author " +
                    "NATURAL JOIN author");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int isbn = rs.getInt("isbn");
                String title = rs.getString("title");
                int edition = rs.getInt("edition_no");
                int numCop = rs.getInt("numofcop");
                int copLeft = rs.getInt("numleft");
                String author = rs.getString("surname");

                catalogue += String.format("%d: %s\n" +
                        "\tEdition: %d - Number of copies: %d - Copies left: %d\n" +
                        "\tAuthor: %s\n\n", isbn, title, edition, numCop, copLeft, author);
            }

            closeStatements(ps, rs);
            if (catalogue != null)
                return "Catalogue:\n" + catalogue;

        }catch (Exception e) {
            return this.ERROR_RETRV_RECORD;
        }

        return "Database contains no catalogue";
    }

    public String showLoanedBooks() {
        String loaned = "";
        try {
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT isbn, title, edition_no, numofcop, numleft, surname, customerid, l_name, f_name " +
                    "FROM book NATURAL JOIN book_author " +
                    "NATURAL JOIN author " +
                    "NATURAL JOIN cust_book " +
                    "NATURAL JOIN customer");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int isbn = rs.getInt("isbn");
                String title = rs.getString("title");
                int edition = rs.getInt("edition_no");
                int numCop = rs.getInt("numofcop");
                int copLeft = rs.getInt("numleft");
                String author = rs.getString("surname");
                String cust_name = rs.getString("f_name") + " " + rs.getString("l_name");
                int custid = rs.getInt("customerid");

                loaned += String.format("\t%d: %s\n" +
                        "\tEdition: %d - Number of copies: %d - Copies left: %d\n" +
                        "\tAuthor: %s\n" +
                        "\tBorrowers:\n" +
                        "\t\t%d: %s", isbn, title, edition, numCop, copLeft, author, custid, cust_name);
            }

            closeStatements(ps, rs);
            if (!loaned.isEmpty())
                return "Loaned Books:\n" + loaned;

        } catch (Exception e) {
            return this.ERROR_RETRV_RECORD;
        }

        return "No books currently on loan";
    }

    public String showAuthor(int authorID) {
        String books = "";
        try {
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT authorid, name || ' ' || surname AS full_name, isbn, title " +
                    "FROM author " +
                    "   NATURAL JOIN book_author " +
                    "   NATURAL JOIN book " +
                    "WHERE authorid = ?");
            ps.setInt(1, authorID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int isbn = rs.getInt("isbn");
                String title = rs.getString("title");
                String name = rs.getString("full_name");
                int authorid = rs.getInt("authorid");

                books += String.format("" +
                        "\t%d: %s\n" +
                        "\tBooks Written:\n", authorid, name);

                if (isbn != 0 && title != null)
                    books += addBook(isbn, title);
                else
                    books += "\t\t--";

                // Continually add the book information
                while (1==1){
                    if (rs.next())
                        books += addBook(rs.getInt("isbn"), rs.getString("title"));
                    else
                        break;
                }
            }

            closeStatements(ps, rs);
            if (!books.isEmpty())
                return "Author:\n" + books;

        } catch (Exception e) {
            return this.ERROR_RETRV_RECORD;
        }

	    return "No Author under the given id: " + authorID;
    }

    /**
     * Helper method to add another book to an entry
     * @param isbn
     * @param title
     * @return "isbn - title"
     */
    private String addBook(int isbn, String title){
        return String.format("\t\t%d - %s\n", isbn, title);
    }

    public String showAllAuthors() {
        StringBuffer authors = new StringBuffer();
        try {
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT authorid, surname || ', ' || name as full_name " +
                    "FROM author");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int authorid = rs.getInt("authorid");
                String name = rs.getString("full_name");

                if (authorid != 0) {
                    String token = String.format("\t%d: %s\n", authorid, name);
                    authors.append(token);
                }
            }

            closeStatements(ps, rs);
            if (authors.length() == 0)
                return "All Authors:\n" + authors;

        } catch (Exception e) {
            return ERROR_RETRV_RECORD;
        }

        return "No authors in Database";
    }

    /**
     * Either gets a customer or all customers
     * @param customerID
     * @return
     * @throws SQLException
     */
    private List<Object> getCustomer(int customerID) throws SQLException {
        PreparedStatement ps = null;

        // Choose either a customer, or all customers
        if (customerID != -1) {
            ps = this.conn.prepareStatement("" +
                    "SELECT customerid || ': ' || l_name || ', ' || f_name || ' - ' || city AS fullDeets " +
                    "FROM customer " +
                    "WHERE customerid = ?");
            ps.setInt(1, customerID);
        } else {
            ps = this.conn.prepareStatement("" +
                    "SELECT customerid || ': ' || l_name || ', ' || f_name || ' - ' || city AS fullDeets " +
                    "FROM customer " +
                    "WHERE customerid > 0");
        }
        ResultSet rs = ps.executeQuery();

        // We use an arraylist because our size can be anything,
        // for the second case
        List<Object> customer = new ArrayList<>();
        while (rs.next()){
            customer.add(rs.getString("fullDeets"));
        }

        closeStatements(ps, rs);
        return customer.isEmpty() ? null : customer;
    }

    public String showCustomer(int customerID) {
        String error = "Cannot find customer with given id: " + customerID;
        String cust = "";
        String books = "";
        try {

            if (customerID == -1 || customerID == 0)
                throw new SQLException();

            // get the customer
            List<Object> customer = getCustomer(customerID);

            if (customer != null)
                cust = String.format("Show Customer:\n\t%s\n", customer.get(0).toString().trim());
            else
                return error;

            // get all items loaned by this customer
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT customerid, isbn, title " +
                    "FROM book " +
                    "NATURAL JOIN cust_book " +
                    "WHERE customerid = ?");
            ps.setInt(1, customerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int isbn = rs.getInt("isbn");
                String title = rs.getString("title");
                books += addBook(isbn, title);
            }

            closeStatements(ps, rs);
            if (!books.isEmpty())
                cust += "\tBooks Borrowed:\n" + books;
            else
                cust += "\tNo Books Borrowed";

        } catch (SQLException e) {
            return ERROR_RETRV_RECORD;
        }

	    return cust;
    }

    public String showAllCustomers() {
        StringBuffer cust = new StringBuffer();
        cust.append("No customer relations found");
        try {
            List<Object> obj = getCustomer(-1);

            if (obj != null) {
                cust = new StringBuffer();
                cust.append("Showing all customers:\n");
                for (int i=0; i<obj.size(); i++) {
                    String token = String.format("\t%s\n", obj.get(i).toString().trim());
                    cust.append(token);
                }
            }

        } catch (SQLException e) {
            return ERROR_RETRV_RECORD;
        }

	    return cust.toString();
    }

    public String borrowBook(int isbn, int customerID,
			     int day, int month, int year) {
	return "Borrow Book Stub";
    }

    public String returnBook(int isbn, int customerid) {
	return "Return Book Stub";
    }

    public void closeDBConnection() {
        try {
            this.conn.close();
        } catch (Exception e) {
            System.out.println("Error closing database");
        }
    }
    
    public String deleteCus(int customerID) {
    	return "Delete Customer";
    }
    
    public String deleteAuthor(int authorID) {
    	return "Delete Author";
    }
    
    public String deleteBook(int isbn) {
    	return "Delete Book";
    }
}