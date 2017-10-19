/*
 * LibraryModel.java
 * Author:
 * Created on:
 */



import javax.swing.*;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class LibraryModel {

    // For use in creating dialogs and making them modal
    private JFrame dialogParent;
    private Connection conn;
    private String databaseURL;

    private final String ERROR_RETRV_RECORD = "Error: retrieving record from DB\n";
    private final String ERROR_CONN_DB = "Error: connecting to database, check username/password\n";

    public LibraryModel(JFrame parent, String userid, String password) {
        dialogParent = parent;

        this.databaseURL = String.format("jdbc:postgresql://localhost:5432/%s_jdbc?user=%s&password=%s",
                userid, userid, password);
        try {
            this.conn = DriverManager.getConnection(this.databaseURL);

        } catch (SQLException e) {
            System.out.println(ERROR_CONN_DB);
            System.exit(-1);
        }

        System.out.println("Successfully connected to db");
    }

    /***********************
     *** BOOK STUB *********
     ***********************/

    public String bookLookup(int isbn) {
        String item = "";
        try {

            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT isbn, title, edition_no, numofcop, numleft, " +
                    "   surname || ', ' || author.name AS full_name " +
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
                String author = rs.getString("full_name");
                item = String.format("\t%d: %s\n" +
                        "\tEdition: %d - Number of copies: %d - Copies left: %d\n" +
                        "\tAuthor: %s", isbn, title, edition, numCop, copLeft, author);
            }

            closeStatements(ps, rs);
            if (item != null)
                return "Book Lookup:\n" + item;

        }catch (Exception e) {
            return this.ERROR_RETRV_RECORD;
        }
        return error_BookNotFound(isbn);
    }

    public String showCatalogue() {
        String catalogue = "";
        try {
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT isbn, title, edition_no, numofcop, numleft, " +
                    "   surname || ', ' || author.name AS full_name " +
                    "FROM book " +
                    "NATURAL JOIN book_author " +
                    "NATURAL JOIN author");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int isbn = rs.getInt("isbn");
                String title = rs.getString("title");
                int edition = rs.getInt("edition_no");
                int numCop = rs.getInt("numofcop");
                int copLeft = rs.getInt("numleft");
                String author = rs.getString("full_name");

                catalogue += String.format("%d: %s\n" +
                        "\tEdition: %d - Number of copies: %d - Copies left: %d\n" +
                        "\tAuthor: %s\n\n", isbn, title, edition, numCop, copLeft, author);
            }

            closeStatements(ps, rs);
            if (catalogue != null)
                return "Show Catalogue:\n" + catalogue;

        }catch (Exception e) {
            return this.ERROR_RETRV_RECORD;
        }

        return "Database contains no catalogue";
    }

    public String showLoanedBooks() {
        String loaned = "";
        try {
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT isbn, title, edition_no, numofcop, numleft, surname || ', ' || name AS author, " +
                    "       customerid, l_name || ', ' || f_name AS full_name " +
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
                String author = rs.getString("author");
                String cust_name = rs.getString("full_name");
                int custid = rs.getInt("customerid");

                loaned += String.format("\t%d: %s\n" +
                        "\tEdition: %d - Number of copies: %d - Copies left: %d\n" +
                        "\tAuthor: %s\n" +
                        "\tBorrowers:\n" +
                        "\t\t%d: %s\n\n", isbn, title, edition, numCop, copLeft, author, custid, cust_name);
            }

            closeStatements(ps, rs);
            if (!loaned.isEmpty())
                return "Loaned Books:\n" + loaned;

        } catch (Exception e) {
            return this.ERROR_RETRV_RECORD;
        }

        return "No books currently on loan";
    }

    /***********************
     *** AUTHOR STUB *******
     ***********************/

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
                    books += addBookString(isbn, title);
                else
                    books += "\t\t--";

                // Continually add the book information
                while (1==1){
                    if (rs.next())
                        books += addBookString(rs.getInt("isbn"), rs.getString("title"));
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

                if (authorid != 0) {        // removes the default name
                    String token = String.format("\t%d: %s\n", authorid, name);
                    authors.append(token);
                }
            }

            closeStatements(ps, rs);
            if (authors.length() != 0)
                return "All Authors:\n" + authors;

        } catch (Exception e) {
            return ERROR_RETRV_RECORD;
        }

        return "No authors in Database";
    }

    /***********************
     *** CUSTOMER STUB *****
     ***********************/

    public String showCustomer(int customerID) {
        String error = "Cannot find customer with given id: " + customerID;
        String cust = "";
        String books = "";
        try {

            if (customerID == -1 || customerID == 0)
                throw new SQLException();

            // get the customer
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT customerid || ': ' || l_name || ', ' || f_name || ' - ' || city AS fullDeets " +
                    "FROM customer " +
                    "WHERE customerid = ?");
            ps.setInt(1, customerID);
            List<Object> customer = getCustomer(ps);

            if (customer != null)
                cust = String.format("Show Customer:\n\t%s\n", customer.get(0).toString().trim());
            else
                return error;

            // get all items loaned by this customer
            PreparedStatement ps2 = this.conn.prepareStatement("" +
                    "SELECT customerid, isbn, title " +
                    "FROM book " +
                    "NATURAL JOIN cust_book " +
                    "WHERE customerid = ?");
            ps2.setInt(1, customerID);
            ResultSet rs = ps2.executeQuery();

            while (rs.next()) {
                int isbn = rs.getInt("isbn");
                String title = rs.getString("title");
                books += addBookString(isbn, title);
            }

            closeStatements(ps, rs);
            if (!books.isEmpty())
                cust += "\tBooks Borrowed:\n" + books;
            else
                cust += "\tNo Books Borrowed";

            return cust;

        } catch (SQLException e) {
            return ERROR_RETRV_RECORD;
        }
    }

    public String showAllCustomers() {
        StringBuffer cust = new StringBuffer();
        cust.append("No customer relations found");
        try {
            PreparedStatement ps = this.conn.prepareStatement("" +
                    "SELECT customerid || ': ' || l_name || ', ' || f_name || ' - ' || city AS fullDeets " +
                    "FROM customer " +
                    "WHERE customerid > 0");

            List<Object> obj = getCustomer(ps);

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

    /***********************
     *** BORROW-BOOK STUB **
     ***********************/

    public String borrowBook(int isbn, int customerID, int day, int month, int year) {

        String error_BookIsLoaned = String.format("The book %d is already loaned by customer %d\n", isbn, customerID);

        try {
            // validate isbn
            if (!validateIsbn(isbn))
                return (error_BookNotFound(isbn));

            // validate customer
            if (!validateCustomer(customerID))
                return (error_CustomerNotFound(customerID));

            // check if book is not loaned
            if (validateLoanedBook(isbn, customerID))
                return (error_BookIsLoaned);

             return borrowBookMethod(isbn, customerID, day, month+1, year);
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    private String borrowBookMethod(int isbn, int customerID, int day, int month, int year) throws SQLException {

        this.conn.setAutoCommit(false);

        String error_invalidDate = String.format("The date %d/%d/%d is invalid\n", day, month, year);

        // Queries
        String insertBook = "INSERT INTO " +
                "   cust_book (isbn, duedate, customerid) VALUES (?, ?, ?)";

        String results = "SELECT title, f_name || ' ' || l_name AS full_name FROM cust_book " +
                "NATURAL JOIN book " +
                "NATURAL JOIN customer " +
                "WHERE isbn = ? AND customerid = ?";

        String bookQuery = "SELECT * FROM book WHERE isbn = ? FOR UPDATE"; // lock the book

        PreparedStatement loanBook = null;
        PreparedStatement updateCopies = null;
        PreparedStatement getDetails = null;
        PreparedStatement bookTable = null;
        ResultSet bookTableResult = null;
        ResultSet details = null;

        // Check if date is valid
        java.sql.Date dueDate = validateDate(day, month, year);
        if (dueDate == null)
            throw new SQLException(error_invalidDate);

        // Save point to roll back to
        Savepoint save1 = this.conn.setSavepoint();

        try {

            // Validate copies
            bookTable = this.conn.prepareStatement(bookQuery);
            bookTable.setInt(1, isbn);
            bookTableResult = bookTable.executeQuery();

            int copies = 0;
            if (bookTableResult.next()) {
                copies = bookTableResult.getInt("numofcop");
                if (copies <= 0)
                    throw new SQLException(error_BookOnLoan(isbn));
            } else
                throw new SQLException(error_BookNotFound(isbn));

            // loan the book out to the customer
            loanBook = this.conn.prepareStatement(insertBook);
            loanBook.setInt(1, isbn);
            loanBook.setDate(2, dueDate);
            loanBook.setInt(3, customerID);
            loanBook.executeUpdate();

            // update the book count
           updateBookCount(bookTable, -1, isbn);

            // Display the results to the user
            getDetails = this.conn.prepareStatement(results);
            getDetails.setInt(1, isbn);
            getDetails.setInt(2, customerID);
            details = getDetails.executeQuery();

            String title = "";
            String customer = "";
            if (details.next()) {
                title = details.getString("title").trim();
                customer = details.getString("full_name").trim();
            } else
                throw new SQLException("Error retrieving details from database, rolling back\n");

            this.conn.commit();
            return String.format("Borrow Book:\n" +
                        "\tBook: %d (%s)\n" +
                        "\tLoaned to: %d (%s)\n" +
                        "\tDue Date: %d %d %d\n", isbn, title, customerID, customer, day, month, year);

        } catch (SQLException e) {
            this.conn.rollback(save1);
            throw e;

        } finally {
            if (loanBook != null) loanBook.close();
            if (getDetails != null) getDetails.close();
            if (updateCopies != null) updateCopies.close();
            if (details != null) details.close();
            if (bookTable != null) bookTable.close();
            if (bookTableResult != null) bookTableResult.close();
            this.conn.setAutoCommit(true);
        }
    }

    public String returnBook(int isbn, int customerid) {
	    try {
	        return returnBookMethod(isbn, customerid);
        } catch (SQLException e) {
	        return e.getMessage();
        }
    }

    private String returnBookMethod(int isbn, int customerid) throws SQLException {

        String error_BookNotLoaned = String.format("Book %d is not loaned to customer %d\n", isbn, customerid);

        // Queries
        String removeCustBookQuery = "DELETE FROM cust_book WHERE isbn = ? AND customerid = ?";

        PreparedStatement deleteBookStmt = null;
        PreparedStatement updateBookStmt = null;

        this.conn.setAutoCommit(false);
        Savepoint save1 = this.conn.setSavepoint();
        try {
            // validate isbn
            if (!validateIsbn(isbn))
                throw new SQLException(error_BookNotFound(isbn));

            // validate customer
            if (!validateCustomer(customerid))
                throw new SQLException(error_CustomerNotFound(customerid));

            // check book is loaned
            if (!validateLoanedBook(isbn, customerid))
                throw new SQLException(error_BookNotLoaned);

            // delete book from cust_book
            deleteBookStmt = this.conn.prepareStatement(removeCustBookQuery);
            deleteBookStmt.setInt(1, isbn);
            deleteBookStmt.setInt(2, customerid);
            deleteBookStmt.executeUpdate();

            // update count of books
            updateBookCount(updateBookStmt, 1, isbn);

            this.conn.commit();
            return String.format("Return Book:\n" +
                    "\tBook %d returned for customer %d\n", isbn, customerid);

        } catch (SQLException e){
            this.conn.rollback(save1);
            throw e;

        } finally {
            // cleanup
            if (deleteBookStmt != null) deleteBookStmt.close();
            if (updateBookStmt != null) updateBookStmt.close();
            this.conn.setAutoCommit(true);
        }
    }

    /***********************
     *** DELETE STUB *******
     ***********************/
    
    public String deleteCus(int customerID) {
    	 try {
    	     // validate customer
             if (!validateCustomer(customerID))
                 return error_CustomerNotFound(customerID);

             // Now we are a valid customer
             return removeCustomer(customerID);
         } catch (SQLException e){
    	    return e.getMessage();
         }
    }

    private String removeCustomer(int customerID) throws SQLException {

        Savepoint save1 = this.conn.setSavepoint();
        this.conn.setAutoCommit(false);

        PreparedStatement ps = null;

        try {

            // should check for dependencies first

            // try removing the customer
            String deleteCustQuery = "DELETE FROM customer WHERE customerid = ?";
            ps = this.conn.prepareStatement(deleteCustQuery);
            ps.setInt(1, customerID);
            ps.executeUpdate();

            this.conn.commit();
            return String.format("Removed customer %d");

        } catch (SQLException e) {
            this.conn.rollback(save1);
            throw e;
        } finally {
            if (ps != null) ps.close();
            this.conn.setAutoCommit(true);
        }
    }
    
    public String deleteAuthor(int authorID) {

        try {

            // check author exists
            if (!validateAuthor(authorID))
                return error_AuthorNotFound(authorID);

            // remove the author
            return removeAuthor(authorID);

        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    private String removeAuthor(int authorID) throws SQLException {

        // dependencies...
        this.conn.setAutoCommit(false);
        Savepoint save1 = this.conn.setSavepoint();

        String query = "DELETE FROM author WHERE authorid = " + authorID;
        PreparedStatement ps = null;

        try {

            ps = this.conn.prepareStatement(query);
            ps.executeUpdate();

        } catch (SQLException e) {
            this.conn.rollback(save1);
            throw e;

        } finally {
            this.conn.setAutoCommit(true);
            if (ps != null) ps.close();
        }
        return "";
    }
    
    public String deleteBook(int isbn) {

        try {
            // check isbn
            if (!validateIsbn(isbn))
                return error_BookNotFound(isbn);

            // remove the book
            return removeBook(isbn);

        } catch (SQLException e){
            return e.getMessage();
        }
    }

    private String removeBook(int isbn){

        return "";
    }

    /***********************
     *** CONNECTION METHODS *
     ***********************/

    public void closeDBConnection() {
        try {
            this.conn.close();
        } catch (Exception e) {
            System.out.println("Error closing database");
        }
    }

    /***********************
     *** HELPER-METHODS ****
     ***********************/

    /**
     * Helper Method: Either gets a customer or all customers
     * @param ps -  The prepared statement to execute
     * @return An arraylist object containing the data of the specified query
     * @throws SQLException
     */
    private List<Object> getCustomer(PreparedStatement ps) throws SQLException {

        ResultSet rs = ps.executeQuery();

        // We use an Arraylist because our size can be anything,
        // for the second case
        List<Object> customer = new ArrayList<>();
        while (rs.next()){
            customer.add(rs.getString("fullDeets"));
        }

        closeStatements(ps, rs);
        return customer.isEmpty() ? null : customer;
    }

    /**
     * Helper method to add another book to an entry
     * @param isbn
     * @param title
     * @return "isbn - title"
     */
    private String addBookString(int isbn, String title){
        return String.format("\t\t%d - %s\n", isbn, title);
    }

    /**
     * HELPER METHOD - Updates the numLeft column in the table book. Either by adding 1 or subtracting 1
     *
     * @param updateCopies
     * @param count
     * @param isbn
     * @throws SQLException
     */
    private void updateBookCount(PreparedStatement updateCopies, int count, int isbn) throws SQLException {
        String updateBookCount = "UPDATE book " +
                "SET numleft =  book.numleft + ? WHERE book.isbn = ?";

        // update the number of copies in book table
        updateCopies = this.conn.prepareStatement(updateBookCount);
        updateCopies.setInt(1, count);
        updateCopies.setInt(2, isbn);
        updateCopies.executeUpdate();
    }

    /**
     * Helper Method: Closes our Statement and ResultSet in a single line of code
     * @param s -   Prepared Statement to close
     * @param r -   ResultSet to close
     * @throws SQLException
     */
    private void closeStatements(PreparedStatement s, ResultSet r) throws SQLException {
        s.close();
        r.close();
    }

    /**
     * Helper Method - Checks if the given date is a valid date against the current date
     * You are able to loan a book if the due date is either greater than or equal to
     * the current date
     *
     * @param day
     * @param month
     * @param year
     * @return true if valid - false otherwise
     */
    private java.sql.Date validateDate(int day, int month, int year){
        String givenDate = String.format("%d-%d-%d", year, month, day);

        // get current date
        Date ds = new Date();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date d1 = df.parse(givenDate);
            Date d2 = df.parse(df.format(ds));

            if (d1.before(d2))
                return null;
            else
                return new java.sql.Date(df.parse(givenDate).getTime());

        } catch (ParseException e) {
            System.out.println("Error upon validating date!");
        }

        return null;    // should never reach
    }

    // VALIDATING METHODS
    private boolean hasItem(PreparedStatement ps, ResultSet rs) throws SQLException {
        if (rs.next()){
            closeStatements(ps, rs);
            return true;
        }
        closeStatements(ps, rs);
        return false;
    }
    private boolean validateIsbn(int isbn) throws SQLException {
        PreparedStatement ps = this.conn.prepareStatement("" +
                "SELECT * FROM book WHERE isbn = " + isbn);
        ResultSet rs = ps.executeQuery();

        return hasItem(ps, rs);
    }
    private boolean validateCustomer(int customerID) throws SQLException {
        PreparedStatement ps = this.conn.prepareStatement("" +
                "SELECT * FROM customer WHERE customerid = " + customerID);
        ResultSet rs = ps.executeQuery();

        return hasItem(ps, rs);
    }
    private boolean validateLoanedBook(int isbn, int customerid) throws SQLException {
        PreparedStatement ps = this.conn.prepareStatement("" +
                "SELECT * FROM cust_book " +
                "WHERE isbn = ? AND customerid = ?");
        ps.setInt(1, isbn);
        ps.setInt(2, customerid);
        ResultSet rs = ps.executeQuery();

        return hasItem(ps, rs);
    }
    private boolean validateAuthor(int authorid) throws SQLException {
        String query = "SELECT * FROM author WHERE authorid = ?";
        PreparedStatement ps = this.conn.prepareStatement(query);
        ps.setInt(1, authorid);
        ResultSet rs = ps.executeQuery();

        return hasItem(ps, rs);
    }

    // ERROR REPORT METHODS
    private String error_BookOnLoan(int isbn) { return String.format("Not enough copies of %d left\n", isbn); }
    private String error_BookNotFound(int isbn) { return String.format("Book with given isbn: %d not found!\n", isbn); }
    private String error_CustomerNotFound(int customerID) { return String.format("The customer %d was not found\n", customerID); }
    private String error_AuthorNotFound(int authorid) { return String.format("The author %d was not found\n", authorid); }

}