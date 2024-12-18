package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager = new LibraryManager(notificationService, userService);
    libraryManager.addBook("book1", 1);
    libraryManager.addBook("book2", 12);
    libraryManager.addBook("book3_is_over", 0);
  }

  // addBook
  @ParameterizedTest
  @DisplayName("addBook: добавление новой книги")
  @CsvSource({
      "new_book4, 1, 1",
      "new_book5, 7, 7",
      "new_book6, 0, 0"
  })
  void testAddNewBook(String bookId, int quantity, int expectedValue) {
    libraryManager.addBook(bookId, quantity);
    int totalQuantity  = libraryManager.getAvailableCopies(bookId);

    assertEquals(expectedValue, totalQuantity);
  }

  @ParameterizedTest
  @DisplayName("addBook: добавление существующей книги")
  @CsvSource({
      "book1, 4, 5",
      "book2, 8, 20",
      "book3_is_over, 1, 1"
  })
  void testAddExistingBook(String bookId, int quantity, int expectedValue) {
    libraryManager.addBook(bookId, quantity);
    int totalQuantity = libraryManager.getAvailableCopies(bookId);

    assertEquals(expectedValue, totalQuantity);
  }

  // В метод addBook необходимо добавить проверку quantity на корректность
  @ParameterizedTest
  @DisplayName("addBook: добавление книги с отрицательным количеством")
  @CsvSource({
      "book1, -1, 0",
      "book2, -3, 9",
      "book3_is_over, -2, -2",
      "new_book4, -1, -1",
  })
  void testAddBookNegativeQuantity(String bookId, int quantity, int expectedValue) {
    libraryManager.addBook(bookId, quantity);
    int totalQuantity = libraryManager.getAvailableCopies(bookId);

    assertEquals(expectedValue, totalQuantity);
  }

  // В метод addBook необходимо добавить проверку bookId на корректность
  @Test
  @DisplayName("addBook: добавление книги с bookId равным null")
  void testAddBookWithNullId() {
    int quantity = 2;
    libraryManager.addBook(null, quantity);
    int totalQuantity = libraryManager.getAvailableCopies(null);

    assertEquals(quantity, totalQuantity);
  }

  // borrowBook
  @ParameterizedTest
  @DisplayName("borrowBook: user с неактивной учетной записью")
  @CsvSource({
      "book1",
      "book2",
      "book3_is_over",
      "new_book4",
  })
  void testBorrowBookByNotActiveUser(String bookId) {
    String userId = "not_active_user";
    when(userService.isUserActive(userId)).thenReturn(false);
    boolean isBorrowBook = libraryManager.borrowBook(bookId, userId);

    assertFalse(isBorrowBook, "Ожидаем false для неактивного пользователя");
    verify(notificationService).notifyUser(userId, "Your account is not active.");
  }

  @ParameterizedTest
  @DisplayName("borrowBook: user с активной учетной записью, но книга закончилась или ее не было")
  @CsvSource({
      "book3_is_over",
      "new_book4",
      "new_book5",
  })
  void testBorrowBookWhenBookNotAvailable(String bookId) {
    String userId = "active_user";
    when(userService.isUserActive(userId)).thenReturn(true);
    boolean isBorrowBook = libraryManager.borrowBook(bookId, userId);

    assertFalse(isBorrowBook, "Ожидаем false для активного пользователя при отсутствующей книге");
  }

  @ParameterizedTest
  @DisplayName("borrowBook: user с активной учетной записью и книга есть в наличии")
  @CsvSource({
      "book1, 0",
      "book2, 11"
  })
  void testBorrowBookByActiveUser(String bookId, int expectedValue) {
    String userId = "active_user";
    when(userService.isUserActive(userId)).thenReturn(true);
    boolean isBorrowBook = libraryManager.borrowBook(bookId, userId);

    assertTrue(isBorrowBook, "Ожидаем true для активного пользователя при наличии книги");
    verify(notificationService).notifyUser(userId, "You have borrowed the book: " + bookId);

    int totalQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedValue, totalQuantity);
  }

  // В метод addBook необходимо добавить проверку bookId на корректность
  // тогда метод borrowBook будет правильно возвращать false, когда bookId равно null
  // и эту проверку можно будет убрать
  @Test
  @DisplayName("borrowBook: user с активной учетной записью и книга с bookId равным null")
  void testBorrowBookWithNullId() {
    String userId = "active_user";
    int quantity = 2;
    libraryManager.addBook(null, quantity);

    when(userService.isUserActive(userId)).thenReturn(true);
    boolean isBorrowBook = libraryManager.borrowBook(null, userId);

    assertTrue(isBorrowBook, "Ожидаем true для активного пользователя и книге с bookId равным null");
    verify(notificationService).notifyUser(userId, "You have borrowed the book: " + null);
  }

  // returnBook
  @Test
  @DisplayName("returnBook: когда книга не была взята пользователем")
  void testReturnBookWasNotBorrow() {
    String bookId = "book1";
    String userId = "user1";
    boolean isReturnBook = libraryManager.returnBook(bookId, userId);

    assertFalse(isReturnBook, "Ожидаем false, для отсутствующих пользователя и книги");
    assertEquals(1, libraryManager.getAvailableCopies(bookId));
  }

  @Test
  @DisplayName("returnBook: когда книга была взята, но другим пользователем")
  void testReturnBookByAnotherUser() {
    String bookId = "book2";
    String userId1 = "user1";
    String userId2 = "user2";
    when(userService.isUserActive(userId2)).thenReturn(true);
    libraryManager.borrowBook(bookId, userId2);
    boolean isReturnBook = libraryManager.returnBook(bookId, userId1);

    assertFalse(isReturnBook, "Ожидаем false, для книги взятой другим пользователем");
    assertEquals(11, libraryManager.getAvailableCopies(bookId));
  }

  @Test
  @DisplayName("returnBook: когда этим пользователем была взята другая книга")
  void testReturnAnotherBookByUser() {
    String bookId1 = "book1";
    String bookId2 = "book2";
    String userId = "user1";
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook(bookId2, userId);
    boolean isReturnBook = libraryManager.returnBook(bookId1, userId);

    assertFalse(isReturnBook, "Ожидаем false, для пользователя взявшего другую книгу");
    assertEquals(11, libraryManager.getAvailableCopies(bookId2));
  }

  @Test
  @DisplayName("returnBook: когда пользователь взял книгу")
  void testReturnBookByUser() {
    String bookId = "book1";
    String userId = "user1";
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook(bookId, userId);
    boolean isReturnBook = libraryManager.returnBook(bookId, userId);

    assertTrue(isReturnBook, "Ожидаем true, для пользователя взявшего другую книгу");
    assertEquals(1, libraryManager.getAvailableCopies(bookId));
  }

  // getAvailableCopies
  @ParameterizedTest
  @DisplayName("getAvailableCopies: получить количество доступных копий существующих и не существующих книги")
  @CsvSource({
      "book1, 1",
      "book2, 12",
      "book3_is_over, 0",
      "new_book4, 0",
      "new_book5, 0"
  })
  void testGetAvailableCopiesBook(String bookId, int expectedValue) {
    int totalQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedValue, totalQuantity);
  }

  // Тест отрабатывает верно, но лучше в метод getAvailableCopies добавить проверку bookId на корректность
  @Test
  @DisplayName("getAvailableCopies: получить количество доступных копий книги с bookId равным null")
  void testGetAvailableCopiesBookWithNullId() {
    int totalQuantity = libraryManager.getAvailableCopies(null);
    assertEquals(0, totalQuantity);
  }

  // calculateDynamicLateFee
  @Test
  @DisplayName("calculateDynamicLateFee: поймать Exception при отрицательной просрочке")
  void calculateDynamicLateFeeShouldThrowExceptionIf() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, true, true)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @DisplayName("calculateDynamicLateFee: рассчитать динамическую плату за просрочку")
  @CsvSource({
      "0, true,   true,   0",
      "1, false,  false,  0.5",
      "1, false,  true,   0.4",
      "1, true,   false,  0.75",
      "1, true,   true,   0.6",
      "2, false,  false,  1.0",
      "2, false,  true,   0.8",
      "2, true,   false,  1.5",
      "2, true,   true,   1.2",
      "3, false,  false,  1.5",
      "3, false,  true,   1.2",
      "3, true,   false,  2.25",
      "3, true,   true,   1.8"
  })
  void testCalculateDynamicLateFee(int overdueDays, boolean isBestseller, boolean isPremiumMember, double expectedFee) {
    double dynamicLateFee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(expectedFee, dynamicLateFee);
  }
}