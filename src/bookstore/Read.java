package bookstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Read {
	public static boolean login(Connection conn, Scanner scanner) throws SQLException {
		System.out.print("아이디: ");
		String id = scanner.next();
		System.out.print("비밀번호: ");
		String password = scanner.next();

		String sql = "SELECT COUNT(*) AS count FROM member WHERE id = ? AND pwd = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, id);
			pstmt.setString(2, password);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					int count = rs.getInt("count");
					if (count == 1) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static void readData(Connection conn, String userId) throws SQLException {
		String sql = "SELECT * FROM member WHERE id = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, userId);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					System.out.println("이름: " + rs.getString("name") + ", 아이디: " + rs.getString("id") + ", 나이: "
							+ rs.getInt("ages") + // 수정
							", 성별: " + rs.getString("sex") + ", 이메일: " + rs.getString("email") + ", 보유 잔액: "
							+ rs.getInt("money")); // 총 구매내역 또는 총액 을 출력할 예정
				} else {
					System.out.println("해당하는 사용자의 정보가 없습니다.");
				}
			}
		}
	}

	static boolean checkIfIdExists(Connection conn, String id) throws SQLException {
		String sql = "SELECT COUNT(*) AS count FROM member WHERE id = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, id);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					int count = rs.getInt("count");
					return count > 0;
				}
			}
		}
		return false;
	}

	static int getLastMemberNo(Connection conn) throws SQLException {
		String sql = "SELECT MAX(member_no) AS lastMemberNo FROM member"; // 수정
		try (PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
			if (rs.next()) {
				return rs.getInt("lastMemberNo");
			}
		}
		return 0;
	}

	static void readBalance(Connection conn, String userId) throws SQLException {
		String balanceQuery = "SELECT money FROM member WHERE id = ?";
		int currentBalance = 0;

		try (PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery)) {
			balanceStmt.setString(1, userId);
			try (ResultSet rs = balanceStmt.executeQuery()) {
				if (rs.next()) {
					currentBalance = rs.getInt("money");
					System.out.println("현재 잔액: " + currentBalance + "원");
				} else {
					System.out.println("잔액 조회에 실패했습니다.");
				}
			}
		}
	}

	static void executeQuery(Connection conn, String memberId) throws SQLException { // 구매내역 조회
		String sql = "SELECT * " + "FROM MEMBER M " + "LEFT JOIN PURCHASE P ON M.ID = P.ID "
				+ "LEFT JOIN BOOK B ON B.BOOK_ID = P.BOOK_ID " + "WHERE M.ID = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, memberId);
			ResultSet rs = pstmt.executeQuery();

			boolean hasPurchaseHistory = false; // 구매 내역 유무를 확인하기 위한 플래그

			while (rs.next()) {
				hasPurchaseHistory = true; // 구매 내역이 하나라도 있으면 플래그를 true로 설정

				int purchaseNo = rs.getInt("purchase_no");
				if (rs.wasNull()) {
					System.out.println("구매 내역이 없습니다.");
					break;
				}
				System.out.print("Member No: " + rs.getInt("member_no") + "  ");
				System.out.print("ID: " + rs.getString("id") + "  ");
				System.out.print("Name: " + rs.getString("name") + "  ");
				System.out.print("Money: " + rs.getInt("money") + "  ");

				System.out.print("Purchase No: " + purchaseNo + "  ");
				System.out.print("Book ID: " + rs.getString("book_id") + "  ");
				System.out.print("State: " + rs.getString("state") + "  ");

				System.out.print("Title: " + rs.getString("subject") + "  ");
				System.out.print("Price: " + rs.getInt("price") + "  ");
				System.out.println("Author: " + rs.getString("author") + "  ");
			}

			if (!hasPurchaseHistory) { // 구매 내역이 없는 경우
				System.out.println("구매 내역이 없습니다.");
			}
		}
	}

	static void displayFavoriteBooks(Connection conn, String loggedInUserId) throws SQLException {
		String sql = "SELECT * " + "FROM Like_book L " + "LEFT JOIN MEMBER M ON M.ID = L.ID "
				+ "JOIN book B ON B.book_id = L.book_id " + "WHERE L.id = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, loggedInUserId);
			ResultSet rs = pstmt.executeQuery();

			boolean hasFavorites = false;

			while (rs.next()) {
				hasFavorites = true;
				System.out.print("Book ID: " + rs.getString("book_id") + "  ");
				System.out.print("Title: " + rs.getString("subject") + "  ");
				System.out.print("Author: " + rs.getString("author") + "  ");
				System.out.print("Price: " + rs.getInt("price") + "  ");
				System.out.println();
			}

			if (!hasFavorites) {
				System.out.println("관심목록이 없습니다.");
			}
		}
	}

	


	// 구매하기 창에서 북 리스트를 보여주고, 선택하는 경우(bookId가 없을 때)
	static void purchaseBook(Connection conn, Scanner scanner) throws SQLException {
		String userId = Main.loggedInUserId; // 현재 로그인한 사용자의 ID 가져오기
		System.out.println("--------------------------");
		System.out.println("(돌아가려면 0을 입력하세요.)원하는 책의 ID를 입력하세요.");
		scanner = new Scanner(System.in);
		String bookId = scanner.nextLine();
		if ("0".equals(bookId))
			return; // 0 입력 받으면 종료
		String bookInfoSql = "SELECT * FROM Book WHERE BOOK_ID = ?";
		// 책 정보 조회
		try (PreparedStatement pstmt = conn.prepareStatement(bookInfoSql)) {
			pstmt.setString(1, bookId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					int bookPrice = rs.getInt("PRICE");
					System.out.println("책 제목: " + rs.getString("SUBJECT"));
					System.out.println("책 가격: " + bookPrice);

					// 구매 옵션 선택
					System.out.println("1. 바로 결제하기  2. 나중에 결제하기");
					System.out.print("구매 옵션을 선택하세요: ");
					int purchaseOption = scanner.nextInt();

					if (purchaseOption == 1) {
						// 바로 결제하기: 보유 잔액에서 즉시 결제
						processImmediatePayment(conn, userId, bookId, bookPrice);
						return;
					} else if (purchaseOption == 2) {
						// 나중에 결제하기: 결제 대기 상태로 이동
						processDelayedPayment(conn, userId, bookId, bookPrice);
						return;
					} else {
						System.out.println("올바른 옵션을 선택하세요.");
					}
				}
			}
		}
	}

	// 책 조회 후 구매하기 버튼 클릭 시 (bookId를 가지고 오는 경우)
	static void purchaseBook(Connection conn, Scanner scanner, String bookId) throws SQLException {
		String userId = Main.loggedInUserId; // 현재 로그인한 사용자의 ID 가져오기
		String bookInfoSql = "SELECT * FROM Book WHERE BOOK_ID = ?";
		System.out.println("--------------------------");
		// 책 정보 조회
		try (PreparedStatement pstmt = conn.prepareStatement(bookInfoSql)) {
		    pstmt.setString(1, bookId);
		    try (ResultSet rs = pstmt.executeQuery()) {
		        // 결과가 없는 경우 처리
		        if (!rs.next()) {
		            System.out.println("검색된 책이 없습니다.");
		            return; // 메소드 종료
		        }

		        // 결과가 있는 경우 처리
		        int bookPrice = rs.getInt("PRICE");
		        System.out.println("책 제목: " + rs.getString("SUBJECT"));
		        System.out.println("책 가격: " + bookPrice);

		        // 구매 옵션 선택
		        System.out.println("1. 바로 결제하기  2. 나중에 결제하기");
		        System.out.print("구매 옵션을 선택하세요: ");
		        int purchaseOption = scanner.nextInt();

		        if (purchaseOption == 1) {
		            // 바로 결제하기: 보유 잔액에서 즉시 결제
		            processImmediatePayment(conn, userId, bookId, bookPrice);
		            return;
		        } else if (purchaseOption == 2) {
		            // 나중에 결제하기: 결제 대기 상태로 이동
		            processDelayedPayment(conn, userId, bookId, bookPrice);
		            return;
		        } else {
		            System.out.println("올바른 옵션을 선택하세요.");
		        }
		    }
		}
	}

	/*
	 * 책 구매 과정 로직에 트랜잭션 추가 필요함 Connection conn = null; try { conn = getConnection();
	 * // getConnection() 메서드는 데이터베이스와 연결을 설정하는데 사용되는 메서드로 가정합니다.
	 * conn.setAutoCommit(false); // 자동 커밋을 해제하여 트랜잭션을 수동으로 관리합니다.
	 * 
	 * // 로그인된 회원의 아이디를 통해 보유 잔액을 조회합니다. int balance = getBalanceForMember(conn,
	 * memberId); // getBalanceForMember() 메서드는 멤버의 보유 잔액을 조회하는데 사용되는 메서드로 가정합니다.
	 * 
	 * // 책의 가격을 조회합니다. int bookPrice = getBookPrice(conn, bookId); //
	 * getBookPrice() 메서드는 책의 가격을 조회하는데 사용되는 메서드로 가정합니다.
	 * 
	 * if (balance >= bookPrice) { // 보유 잔액이 책의 가격보다 크거나 같은 경우 // 보유 책 리스트에 추가하고 소지
	 * 잔액을 갱신하는 작업을 수행합니다. addToBookList(conn, memberId, bookId); // addToBookList()
	 * 메서드는 보유 책 리스트에 추가하는 작업을 수행하는 메서드로 가정합니다. updateBalance(conn, memberId,
	 * balance - bookPrice); // updateBalance() 메서드는 보유 잔액을 갱신하는 작업을 수행하는 메서드로
	 * 가정합니다. updateBookAmount(conn, bookId); // updateBookAmount() 메서드는 책의 재고를
	 * 감소시키는 작업을 수행하는 메서드로 가정합니다.
	 * 
	 * conn.commit(); // 모든 작업이 정상적으로 수행되었으므로 커밋을 실행합니다. } else {
	 * System.out.println("보유 잔액이 부족합니다."); } } catch (SQLException e) { if (conn !=
	 * null) { try { conn.rollback(); // 예외 발생 시 롤백하여 이전 상태로 되돌립니다. } catch
	 * (SQLException rollbackEx) { rollbackEx.printStackTrace(); } }
	 * e.printStackTrace(); } finally { if (conn != null) { try {
	 * conn.setAutoCommit(true); // 트랜잭션 완료 후 다시 자동 커밋 모드로 변경합니다. conn.close(); //
	 * 연결을 닫습니다. } catch (SQLException closeEx) { closeEx.printStackTrace(); } } }
	 * 
	 */
	static void processImmediatePayment(Connection conn, String userId, String bookId, int bookPrice)
			throws SQLException {
		// 보유 잔액 확인
		int currentBalance = getCurrentBalance(conn, userId); // readbalance 부분이 있지만 void 형태이기에 int로 하나더만들었음
		if (currentBalance < bookPrice) {
			System.out.println("잔액이 부족하여 결제할 수 없습니다.");
			return;
		}

		// 결제 진행
		String updateBalanceSql = "UPDATE Member SET MONEY = MONEY - ? WHERE ID = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(updateBalanceSql)) {
			pstmt.setInt(1, bookPrice);
			pstmt.setString(2, userId);
			int affectedRows = pstmt.executeUpdate();
			if (affectedRows > 0) {
				// 결제 성공 시 구매 내역 추가
				addPurchase(conn, userId, bookId, "결제완료");
				System.out.println("결제가 완료되었습니다.");
				return;
			} else {
				System.out.println("결제에 실패했습니다.");
				return;
			}
		}
	}

	static void processDelayedPayment(Connection conn, String userId, String bookId, int bookPrice)
			throws SQLException {
		// 결제 대기 상태로 이동
		addPurchase(conn, userId, bookId, "결제대기");
		System.out.println("결제 대기 상태로 이동되었습니다.");
	}

	static int getCurrentBalance(Connection conn, String userId) throws SQLException {
		String balanceQuery = "SELECT MONEY FROM Member WHERE ID = ?";
		int currentBalance = 0;

		try (PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery)) {
			balanceStmt.setString(1, userId);
			try (ResultSet rs = balanceStmt.executeQuery()) {
				if (rs.next()) {
					currentBalance = rs.getInt("MONEY");
					System.out.println("현재 보유 잔액 : " + currentBalance + "원 입니다.");
				} else {
					System.out.println("잔액 조회에 실패했습니다.");
				}
			}
		}
		return currentBalance;
	}

	static void addPurchase(Connection conn, String userId, String bookId, String state) throws SQLException {
		String insertSql = "INSERT INTO Purchase (PURCHASE_NO, BOOK_ID, ID, STATE) VALUES (?, ?, ?, ?)";
		try (PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM PURCHASE");
				ResultSet countRs = countStmt.executeQuery();
				PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
			int count = 0;
			if (countRs.next()) {
				count = countRs.getInt("count") + 1;
			}
			pstmt.setInt(1, count);
			pstmt.setString(2, bookId);
			pstmt.setString(3, userId);
			pstmt.setString(4, state);
			int affectedRows = pstmt.executeUpdate();
			if (affectedRows > 0) {
				System.out.println("결제 상태가 업데이트 되었습니다.");
			} else {
				System.out.println("구매 내역 추가에 실패했습니다.");
			}
		}
	}

}
