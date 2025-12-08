package com.foodreview.global.config;

import com.foodreview.domain.chat.entity.ChatMessage;
import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.chat.repository.ChatMessageRepository;
import com.foodreview.domain.chat.repository.ChatRoomRepository;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.repository.RestaurantRepository;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.review.entity.Sympathy;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.review.repository.SympathyRepository;
import com.foodreview.domain.user.entity.Follow;
import com.foodreview.domain.user.entity.ScoreEvent;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.FollowRepository;
import com.foodreview.domain.user.repository.ScoreEventRepository;
import com.foodreview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final SympathyRepository sympathyRepository;
    private final FollowRepository followRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("초기 데이터 로딩 시작...");

        // 비밀번호 해시
        String encodedPassword = passwordEncoder.encode("password123");

        // Users 생성
        List<User> users = createUsers(encodedPassword);
        userRepository.saveAll(users);

        // Restaurants 생성
        List<Restaurant> restaurants = createRestaurants();
        restaurantRepository.saveAll(restaurants);

        // Reviews 생성
        List<Review> reviews = createReviews(users, restaurants);
        reviewRepository.saveAll(reviews);

        // Sympathies 생성
        createSympathies(users, reviews);

        // Follows 생성
        createFollows(users);

        // Chat 생성
        createChats(users);

        // Score Events 생성
        createScoreEvents(users);

        log.info("초기 데이터 로딩 완료!");
    }

    private List<User> createUsers(String encodedPassword) {
        return List.of(
            User.builder().email("user1@foodreview.com").password(encodedPassword).name("맛탐험가").avatar("/korean-food-lover-avatar.jpg").region("서울 강남구").tasteScore(2450).reviewCount(156).favoriteCategories(List.of("한식", "일식", "파스타")).build(),
            User.builder().email("user2@foodreview.com").password(encodedPassword).name("동네미식가").avatar("/casual-food-reviewer-avatar.jpg").region("서울 마포구").tasteScore(1820).reviewCount(98).favoriteCategories(List.of("중식", "한식", "베이커리")).build(),
            User.builder().email("user3@foodreview.com").password(encodedPassword).name("골목맛집헌터").avatar("/street-food-explorer-avatar.jpg").region("부산 해운대구").tasteScore(1650).reviewCount(87).favoriteCategories(List.of("해산물", "한식", "분식")).build(),
            User.builder().email("user4@foodreview.com").password(encodedPassword).name("카페투어러").avatar("/cafe-lover-avatar.jpg").region("서울 성동구").tasteScore(1420).reviewCount(64).favoriteCategories(List.of("카페", "디저트", "베이커리")).build(),
            User.builder().email("user5@foodreview.com").password(encodedPassword).name("야식킹").avatar("/late-night-food-avatar.jpg").region("서울 영등포구").tasteScore(1280).reviewCount(52).favoriteCategories(List.of("치킨", "피자", "중식")).build(),
            User.builder().email("user6@foodreview.com").password(encodedPassword).name("베이킹마스터").avatar("/baker-avatar.jpg").region("서울 서초구").tasteScore(1180).reviewCount(45).favoriteCategories(List.of("베이커리", "카페", "디저트")).build(),
            User.builder().email("user7@foodreview.com").password(encodedPassword).name("분식러버").avatar("/korean-street-food-lover-avatar.jpg").region("서울 동작구").tasteScore(1050).reviewCount(38).favoriteCategories(List.of("분식", "한식", "중식")).build(),
            User.builder().email("user8@foodreview.com").password(encodedPassword).name("일식덕후").avatar("/japanese-food-lover-avatar.jpg").region("서울 강남구").tasteScore(980).reviewCount(42).favoriteCategories(List.of("일식", "양식", "카페")).build(),
            User.builder().email("user9@foodreview.com").password(encodedPassword).name("짬뽕매니아").avatar("/chinese-noodle-lover-avatar.jpg").region("인천 중구").tasteScore(920).reviewCount(33).favoriteCategories(List.of("중식", "한식", "분식")).build(),
            User.builder().email("user10@foodreview.com").password(encodedPassword).name("디저트헌터").avatar("/dessert-lover-avatar.jpg").region("서울 강서구").tasteScore(870).reviewCount(29).favoriteCategories(List.of("카페", "베이커리", "양식")).build(),
            User.builder().email("user11@foodreview.com").password(encodedPassword).name("해장의신").avatar("/hangover-food-expert-avatar.jpg").region("대구 중구").tasteScore(820).reviewCount(27).favoriteCategories(List.of("한식", "분식", "중식")).build(),
            User.builder().email("user12@foodreview.com").password(encodedPassword).name("파스타장인").avatar("/pasta-chef-avatar.jpg").region("서울 용산구").tasteScore(760).reviewCount(24).favoriteCategories(List.of("양식", "일식", "카페")).build(),
            User.builder().email("user13@foodreview.com").password(encodedPassword).name("빵순이").avatar("/bread-lover-girl-avatar.jpg").region("대전 서구").tasteScore(710).reviewCount(21).favoriteCategories(List.of("베이커리", "카페", "분식")).build(),
            User.builder().email("user14@foodreview.com").password(encodedPassword).name("라멘오타쿠").avatar("/ramen-lover-avatar.jpg").region("서울 마포구").tasteScore(650).reviewCount(19).favoriteCategories(List.of("일식", "중식", "한식")).build(),
            User.builder().email("user15@foodreview.com").password(encodedPassword).name("떡볶이킹").avatar("/tteokbokki-lover-avatar.jpg").region("부산 서면").tasteScore(580).reviewCount(16).favoriteCategories(List.of("분식", "한식", "중식")).build()
        );
    }

    private List<Restaurant> createRestaurants() {
        return List.of(
            Restaurant.builder().name("할매순대국").category(Restaurant.Category.KOREAN).address("서울 강남구 역삼동 123-45").region("서울 강남구").thumbnail("/korean-sundae-soup-restaurant.jpg").averageRating(new BigDecimal("4.8")).reviewCount(234).priceRange("8,000원 ~ 12,000원").build(),
            Restaurant.builder().name("스시오마카세").category(Restaurant.Category.JAPANESE).address("서울 마포구 서교동 456-78").region("서울 마포구").thumbnail("/restaurant-japanese-1.png").averageRating(new BigDecimal("4.6")).reviewCount(0).priceRange("80,000원 ~ 150,000원").build(),
            Restaurant.builder().name("명동교자").category(Restaurant.Category.KOREAN).address("서울 중구 명동 789-12").region("서울 중구").thumbnail("/korean-dumpling-noodle-restaurant.jpg").averageRating(new BigDecimal("4.5")).reviewCount(567).priceRange("10,000원 ~ 15,000원").build(),
            Restaurant.builder().name("진진반점").category(Restaurant.Category.CHINESE).address("부산 해운대구 우동 234-56").region("부산 해운대구").thumbnail("/chinese-jjajangmyeon-restaurant.jpg").averageRating(new BigDecimal("4.3")).reviewCount(0).priceRange("7,000원 ~ 20,000원").build(),
            Restaurant.builder().name("파스타공방").category(Restaurant.Category.WESTERN).address("서울 성동구 성수동 567-89").region("서울 성동구").thumbnail("/italian-pasta-restaurant-seongsu.jpg").averageRating(new BigDecimal("4.7")).reviewCount(189).priceRange("15,000원 ~ 28,000원").build(),
            Restaurant.builder().name("진미평양냉면").category(Restaurant.Category.KOREAN).address("서울 마포구 연남동 234-12").region("서울 마포구").thumbnail("/korean-cold-noodle-naengmyeon-restaurant.jpg").averageRating(new BigDecimal("4.7")).reviewCount(312).priceRange("12,000원 ~ 16,000원").build(),
            Restaurant.builder().name("멘야하나비").category(Restaurant.Category.JAPANESE).address("서울 강남구 신사동 567-34").region("서울 강남구").thumbnail("/japanese-ramen-restaurant.png").averageRating(new BigDecimal("4.4")).reviewCount(189).priceRange("12,000원 ~ 18,000원").build(),
            Restaurant.builder().name("규카츠명가").category(Restaurant.Category.JAPANESE).address("서울 성동구 성수동 123-56").region("서울 성동구").thumbnail("/japanese-gyukatsu-beef-cutlet-restaurant.jpg").averageRating(new BigDecimal("4.5")).reviewCount(0).priceRange("18,000원 ~ 28,000원").build(),
            Restaurant.builder().name("홍보각").category(Restaurant.Category.CHINESE).address("인천 중구 차이나타운 45-12").region("인천 중구").thumbnail("/chinese-restaurant-jjamppong.jpg").averageRating(new BigDecimal("4.6")).reviewCount(423).priceRange("8,000원 ~ 25,000원").build(),
            Restaurant.builder().name("류샹").category(Restaurant.Category.CHINESE).address("서울 용산구 이태원동 234-78").region("서울 용산구").thumbnail("/authentic-sichuan-chinese-restaurant.jpg").averageRating(new BigDecimal("4.4")).reviewCount(156).priceRange("15,000원 ~ 40,000원").build(),
            Restaurant.builder().name("더스테이크하우스").category(Restaurant.Category.WESTERN).address("서울 강남구 청담동 456-23").region("서울 강남구").thumbnail("/premium-steakhouse-restaurant.jpg").averageRating(new BigDecimal("4.8")).reviewCount(278).priceRange("50,000원 ~ 120,000원").build(),
            Restaurant.builder().name("브런치카페라라").category(Restaurant.Category.WESTERN).address("서울 서초구 반포동 123-45").region("서울 서초구").thumbnail("/brunch-cafe-lala.jpg").averageRating(new BigDecimal("4.3")).reviewCount(0).priceRange("18,000원 ~ 32,000원").build(),
            Restaurant.builder().name("커피한약방").category(Restaurant.Category.CAFE).address("서울 종로구 익선동 12-34").region("서울 종로구").thumbnail("/hanok-cafe-ikseondong.jpg").averageRating(new BigDecimal("4.6")).reviewCount(345).priceRange("5,000원 ~ 9,000원").build(),
            Restaurant.builder().name("포레스트").category(Restaurant.Category.CAFE).address("서울 성동구 성수동 789-12").region("서울 성동구").thumbnail("/forest-cafe-seongsu.jpg").averageRating(new BigDecimal("4.5")).reviewCount(234).priceRange("6,000원 ~ 12,000원").build(),
            Restaurant.builder().name("루프탑커피").category(Restaurant.Category.CAFE).address("서울 용산구 한남동 456-78").region("서울 용산구").thumbnail("/rooftop-cafe-hannam.jpg").averageRating(new BigDecimal("4.4")).reviewCount(0).priceRange("7,000원 ~ 15,000원").build(),
            Restaurant.builder().name("밀도").category(Restaurant.Category.BAKERY).address("서울 강남구 도산대로 234-56").region("서울 강남구").thumbnail("/mildo-bakery-bread.jpg").averageRating(new BigDecimal("4.7")).reviewCount(456).priceRange("4,000원 ~ 15,000원").build(),
            Restaurant.builder().name("테일러커피앤베이커리").category(Restaurant.Category.BAKERY).address("서울 마포구 망원동 123-45").region("서울 마포구").thumbnail("/taylor-coffee-bakery.jpg").averageRating(new BigDecimal("4.5")).reviewCount(289).priceRange("3,500원 ~ 12,000원").build(),
            Restaurant.builder().name("르팡도레").category(Restaurant.Category.BAKERY).address("대전 서구 둔산동 567-89").region("대전 서구").thumbnail("/le-pain-dore-bakery.jpg").averageRating(new BigDecimal("4.6")).reviewCount(0).priceRange("4,500원 ~ 18,000원").build(),
            Restaurant.builder().name("신당동떡볶이").category(Restaurant.Category.SNACK).address("서울 중구 신당동 345-67").region("서울 중구").thumbnail("/sindang-tteokbokki.jpg").averageRating(new BigDecimal("4.4")).reviewCount(567).priceRange("4,000원 ~ 12,000원").build(),
            Restaurant.builder().name("마포원조김밥").category(Restaurant.Category.SNACK).address("서울 마포구 공덕동 234-56").region("서울 마포구").thumbnail("/mapo-kimbap.jpg").averageRating(new BigDecimal("4.3")).reviewCount(234).priceRange("3,000원 ~ 8,000원").build(),
            Restaurant.builder().name("부산어묵당").category(Restaurant.Category.SNACK).address("부산 서면 456-78").region("부산 서면").thumbnail("/busan-fish-cake.jpg").averageRating(new BigDecimal("4.5")).reviewCount(0).priceRange("5,000원 ~ 15,000원").build()
        );
    }

    private List<Review> createReviews(List<User> users, List<Restaurant> restaurants) {
        return List.of(
            Review.builder().user(users.get(0)).restaurant(restaurants.get(0)).content("40년 전통의 맛! 진한 국물에 순대도 신선하고 양도 푸짐합니다. 아침 출근 전에 먹기 딱 좋아요. 특히 막창이 들어간 모듬순대국 강추!").rating(new BigDecimal("5.0")).images(List.of("/korean-sundae-soup-delicious.jpg")).menu("모듬순대국").price("10,000원").visitDate(LocalDate.of(2024, 12, 1)).sympathyCount(156).isFirstReview(false).build(),
            Review.builder().user(users.get(1)).restaurant(restaurants.get(2)).content("손으로 빚은 만두가 정말 맛있어요. 칼국수 면발도 쫄깃하고 국물이 깔끔해서 자주 갑니다. 점심시간에는 웨이팅이 있으니 참고하세요!").rating(new BigDecimal("4.0")).images(List.of("/korean-dumpling-kalguksu.jpg")).menu("만두칼국수").price("12,000원").visitDate(LocalDate.of(2024, 11, 28)).sympathyCount(89).isFirstReview(false).build(),
            Review.builder().user(users.get(2)).restaurant(restaurants.get(4)).content("성수동에서 파스타 맛집 찾는다면 여기! 생면 파스타에 트러플 오일 향이 진하게 나요. 분위기도 좋아서 데이트 코스로 추천드립니다.").rating(new BigDecimal("5.0")).images(List.of("/truffle-pasta-italian.jpg")).menu("트러플 크림 파스타").price("24,000원").visitDate(LocalDate.of(2024, 12, 3)).sympathyCount(234).isFirstReview(false).build(),
            Review.builder().user(users.get(7)).restaurant(restaurants.get(6)).content("진한 돈코츠 육수에 챠슈가 입에서 녹아요. 면 익힘도 선택 가능하고 무한리필 김치도 맛있습니다. 라멘 좋아하시는 분들 강추!").rating(new BigDecimal("5.0")).images(List.of("/japanese-ramen-restaurant.png")).menu("특제 돈코츠라멘").price("14,000원").visitDate(LocalDate.of(2024, 12, 1)).sympathyCount(78).isFirstReview(false).build(),
            Review.builder().user(users.get(8)).restaurant(restaurants.get(8)).content("인천 차이나타운에서 짬뽕 맛집으로 유명한 곳! 얼큰한 국물에 해물이 푸짐하게 들어가요. 탕수육도 바삭바삭 찍먹파라면 여기!").rating(new BigDecimal("5.0")).images(List.of("/chinese-restaurant-jjamppong.jpg")).menu("삼선짬뽕").price("12,000원").visitDate(LocalDate.of(2024, 11, 25)).sympathyCount(145).isFirstReview(false).build(),
            Review.builder().user(users.get(3)).restaurant(restaurants.get(12)).content("익선동 한옥 카페! 인테리어가 정말 예쁘고 시그니처 한방차가 독특해요. 사진 찍기 좋고 조용해서 대화하기도 좋습니다.").rating(new BigDecimal("4.0")).images(List.of("/hanok-cafe-ikseondong.jpg")).menu("오미자차").price("7,000원").visitDate(LocalDate.of(2024, 11, 30)).sympathyCount(67).isFirstReview(false).build(),
            Review.builder().user(users.get(5)).restaurant(restaurants.get(15)).content("식빵이 진짜 유명한 곳! 줄 서서 먹는 이유가 있어요. 갓 구운 식빵의 버터향이 정말 좋고 질감도 완벽합니다.").rating(new BigDecimal("5.0")).images(List.of("/mildo-bakery-bread.jpg")).menu("우유식빵").price("8,000원").visitDate(LocalDate.of(2024, 12, 2)).sympathyCount(198).isFirstReview(false).build(),
            Review.builder().user(users.get(6)).restaurant(restaurants.get(18)).content("신당동 떡볶이 골목의 원조집! 쫄깃한 밀떡에 달달매콤한 양념이 중독적이에요. 튀김이랑 순대도 같이 드시면 완벽!").rating(new BigDecimal("5.0")).images(List.of("/sindang-tteokbokki.jpg")).menu("떡볶이 세트").price("9,000원").visitDate(LocalDate.of(2024, 11, 28)).sympathyCount(112).isFirstReview(false).build(),
            Review.builder().user(users.get(11)).restaurant(restaurants.get(10)).content("드라이에이징 스테이크의 정점! 미디엄 레어로 구워주시는데 육즙이 장난 아니에요. 특별한 날 가기 딱 좋은 곳입니다.").rating(new BigDecimal("5.0")).images(List.of("/premium-steakhouse-restaurant.jpg")).menu("드라이에이징 립아이").price("85,000원").visitDate(LocalDate.of(2024, 11, 20)).sympathyCount(89).isFirstReview(false).build(),
            Review.builder().user(users.get(10)).restaurant(restaurants.get(5)).content("평양냉면 입문자에게 추천! 담백한 육수에 쫄깃한 면발이 일품이에요. 수육도 부드럽고 겨자는 살짝만 넣는 게 포인트!").rating(new BigDecimal("4.0")).images(List.of("/korean-cold-noodle-naengmyeon-restaurant.jpg")).menu("물냉면").price("14,000원").visitDate(LocalDate.of(2024, 12, 1)).sympathyCount(56).isFirstReview(false).build()
        );
    }

    private void createSympathies(List<User> users, List<Review> reviews) {
        List<Sympathy> sympathies = List.of(
            Sympathy.builder().user(users.get(1)).review(reviews.get(0)).build(),
            Sympathy.builder().user(users.get(2)).review(reviews.get(0)).build(),
            Sympathy.builder().user(users.get(0)).review(reviews.get(1)).build(),
            Sympathy.builder().user(users.get(3)).review(reviews.get(2)).build(),
            Sympathy.builder().user(users.get(0)).review(reviews.get(4)).build(),
            Sympathy.builder().user(users.get(1)).review(reviews.get(7)).build()
        );
        sympathyRepository.saveAll(sympathies);
    }

    private void createFollows(List<User> users) {
        List<Follow> follows = List.of(
            Follow.builder().follower(users.get(0)).following(users.get(1)).build(),
            Follow.builder().follower(users.get(0)).following(users.get(2)).build(),
            Follow.builder().follower(users.get(1)).following(users.get(0)).build(),
            Follow.builder().follower(users.get(2)).following(users.get(0)).build(),
            Follow.builder().follower(users.get(3)).following(users.get(0)).build(),
            Follow.builder().follower(users.get(4)).following(users.get(0)).build()
        );
        followRepository.saveAll(follows);
    }

    private void createChats(List<User> users) {
        ChatRoom room1 = ChatRoom.builder()
            .user1(users.get(0)).user2(users.get(1))
            .lastMessage("마포구 신상 맛집 정보 공유해요!")
            .lastMessageAt(LocalDateTime.of(2024, 12, 3, 14, 30))
            .build();
        ChatRoom room2 = ChatRoom.builder()
            .user1(users.get(0)).user2(users.get(2))
            .lastMessage("부산 여행 가는데 추천 부탁드려요~")
            .lastMessageAt(LocalDateTime.of(2024, 12, 2, 10, 15))
            .build();
        chatRoomRepository.saveAll(List.of(room1, room2));

        List<ChatMessage> messages = List.of(
            ChatMessage.builder().chatRoom(room1).sender(users.get(1)).content("안녕하세요! 마포구 맛집 리뷰 잘 봤어요").isRead(true).build(),
            ChatMessage.builder().chatRoom(room1).sender(users.get(0)).content("감사합니다! 혹시 찾으시는 종류 있으세요?").isRead(true).build(),
            ChatMessage.builder().chatRoom(room1).sender(users.get(1)).content("분위기 좋은 파스타집 추천해주세요!").isRead(true).build(),
            ChatMessage.builder().chatRoom(room1).sender(users.get(1)).content("마포구 신상 맛집 정보 공유해요!").isRead(false).build(),
            ChatMessage.builder().chatRoom(room2).sender(users.get(0)).content("부산 여행 가시는군요! 좋겠다~").isRead(true).build(),
            ChatMessage.builder().chatRoom(room2).sender(users.get(2)).content("네! 해운대 쪽 맛집 추천해주세요").isRead(true).build(),
            ChatMessage.builder().chatRoom(room2).sender(users.get(0)).content("부산 여행 가는데 추천 부탁드려요~").isRead(true).build()
        );
        chatMessageRepository.saveAll(messages);
    }

    private void createScoreEvents(List<User> users) {
        List<ScoreEvent> events = List.of(
            ScoreEvent.builder().user(users.get(0)).type(ScoreEvent.ScoreEventType.REVIEW).description("할매순대국 리뷰 작성").points(50).build(),
            ScoreEvent.builder().user(users.get(0)).type(ScoreEvent.ScoreEventType.SYMPATHY_RECEIVED).description("동네미식가님이 공감").points(9).fromUser(users.get(1)).build(),
            ScoreEvent.builder().user(users.get(0)).type(ScoreEvent.ScoreEventType.SYMPATHY_RECEIVED).description("골목맛집헌터님이 공감").points(8).fromUser(users.get(2)).build(),
            ScoreEvent.builder().user(users.get(1)).type(ScoreEvent.ScoreEventType.REVIEW).description("명동교자 리뷰 작성").points(50).build(),
            ScoreEvent.builder().user(users.get(1)).type(ScoreEvent.ScoreEventType.SYMPATHY_RECEIVED).description("맛탐험가님이 공감").points(12).fromUser(users.get(0)).build(),
            ScoreEvent.builder().user(users.get(1)).type(ScoreEvent.ScoreEventType.SYMPATHY_BONUS).description("마스터 맛탐험가님의 공감 보너스").points(25).fromUser(users.get(0)).build()
        );
        scoreEventRepository.saveAll(events);
    }
}
