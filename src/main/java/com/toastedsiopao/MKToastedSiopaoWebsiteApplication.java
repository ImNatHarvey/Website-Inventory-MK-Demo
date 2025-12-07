package com.toastedsiopao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync; // IMPORT ADDED
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.toastedsiopao.model.User;
import com.toastedsiopao.model.Permission;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.InventoryCategory;
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Category;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.RecipeIngredient;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;

import com.toastedsiopao.repository.RoleRepository;
import com.toastedsiopao.repository.UserRepository;
import com.toastedsiopao.repository.InventoryCategoryRepository;
import com.toastedsiopao.repository.UnitOfMeasureRepository;
import com.toastedsiopao.repository.InventoryItemRepository;
import com.toastedsiopao.repository.CategoryRepository;
import com.toastedsiopao.repository.ProductRepository;
import com.toastedsiopao.repository.OrderRepository;

@SpringBootApplication
@EnableScheduling
@EnableAsync // --- ADDED: Enables background processing for emails ---
public class MKToastedSiopaoWebsiteApplication {

	private static final Logger log = LoggerFactory.getLogger(MKToastedSiopaoWebsiteApplication.class);

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private InventoryCategoryRepository invCategoryRepo;
	@Autowired
	private UnitOfMeasureRepository uomRepo;
	@Autowired
	private InventoryItemRepository invItemRepo;
	@Autowired
	private CategoryRepository prodCategoryRepo;
	@Autowired
	private ProductRepository productRepo;
	@Autowired
	private OrderRepository orderRepo;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${mk.admin.username}")
	private String adminUsername;

	@Value("${mk.admin.password}")
	private String adminPassword;

	public static void main(String[] args) {
		SpringApplication.run(MKToastedSiopaoWebsiteApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Manila"));
	}

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {

			// 1. Roles
			final Role ownerRole;
			Optional<Role> ownerRoleOpt = roleRepository.findByName("ROLE_OWNER");
			boolean needsSave = false;
			Role roleToSave;

			if (ownerRoleOpt.isEmpty()) {
				log.info(">>> Creating 'ROLE_OWNER' role...");
				roleToSave = new Role("ROLE_OWNER");
				Arrays.stream(Permission.values()).forEach(permission -> roleToSave.addPermission(permission.name()));
				needsSave = true;
			} else {
				roleToSave = ownerRoleOpt.get();
				long missingPerms = Arrays.stream(Permission.values())
						.filter(permission -> !roleToSave.getPermissions().contains(permission.name())).count();
				if (missingPerms > 0) {
					log.warn(">>> ROLE_OWNER is missing {} permissions. Adding them now...", missingPerms);
					Arrays.stream(Permission.values())
							.forEach(permission -> roleToSave.addPermission(permission.name()));
					needsSave = true;
				}
			}

			if (needsSave) {
				ownerRole = roleRepository.save(roleToSave);
			} else {
				ownerRole = roleToSave;
			}

			Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_CUSTOMER' role...");
				Role newCustomerRole = new Role("ROLE_CUSTOMER");
				return roleRepository.save(newCustomerRole);
			});

			// 2. Users
			Optional<User> existingAdminOptional = userRepository.findByUsername(adminUsername);
			if (existingAdminOptional.isEmpty()) {
				log.info(">>> Creating admin user '{}'", adminUsername);
				User adminUser = new User();
				adminUser.setUsername(adminUsername);
				adminUser.setPassword(passwordEncoder.encode(adminPassword));
				adminUser.setRole(ownerRole);
				adminUser.setFirstName("Admin");
				adminUser.setLastName("User");
				adminUser.setStatus("ACTIVE");
				userRepository.save(adminUser);
			}

			String customerUsername = "testcustomer";
			User customerUser;
			Optional<User> existingCustomer = userRepository.findByUsername(customerUsername);
			if (existingCustomer.isEmpty()) {
				log.info(">>> Creating test customer user '{}'", customerUsername);
				customerUser = new User();
				customerUser.setUsername(customerUsername);
				customerUser.setPassword(passwordEncoder.encode("password123"));
				customerUser.setRole(customerRole);
				customerUser.setFirstName("Test");
				customerUser.setLastName("Customer");
				customerUser.setEmail("test@example.com");
				customerUser.setPhone("09123456789");
				customerUser.setStatus("ACTIVE");
				customerUser = userRepository.save(customerUser);
			} else {
				customerUser = existingCustomer.get();
			}

			// 3. Demo Data Seeding (Inventory & Products)
			Product specialProduct = null;
			Product drinksProduct = null;

			if (invItemRepo.count() == 0) {
				log.info(">>> Seeding Demo Inventory & Products...");

				InventoryCategory rawCat = invCategoryRepo.save(new InventoryCategory("Raw Ingredients"));
				invCategoryRepo.save(new InventoryCategory("Packaging"));

				UnitOfMeasure kg = uomRepo.save(new UnitOfMeasure("Kilogram", "kg"));
				UnitOfMeasure pcs = uomRepo.save(new UnitOfMeasure("Pieces", "pcs"));
				uomRepo.save(new UnitOfMeasure("Pack", "pck"));

				InventoryItem flour = new InventoryItem();
				flour.setName("All-Purpose Flour");
				flour.setCategory(rawCat);
				flour.setUnit(kg);
				flour.setCurrentStock(new BigDecimal("50.00"));
				flour.setCostPerUnit(new BigDecimal("45.00"));
				flour.setLowStockThreshold(new BigDecimal("10.00"));
				invItemRepo.save(flour);

				InventoryItem pork = new InventoryItem();
				pork.setName("Ground Pork");
				pork.setCategory(rawCat);
				pork.setUnit(kg);
				pork.setCurrentStock(new BigDecimal("20.00"));
				pork.setCostPerUnit(new BigDecimal("300.00"));
				pork.setLowStockThreshold(new BigDecimal("5.00"));
				invItemRepo.save(pork);

				InventoryItem egg = new InventoryItem();
				egg.setName("Eggs (Large)");
				egg.setCategory(rawCat);
				egg.setUnit(pcs);
				egg.setCurrentStock(new BigDecimal("100.00"));
				egg.setCostPerUnit(new BigDecimal("10.00"));
				invItemRepo.save(egg);

				Category siopaoCat = prodCategoryRepo.save(new Category("Siopao"));
				Category drinksCat = prodCategoryRepo.save(new Category("Drinks"));
				prodCategoryRepo.save(new Category("Combos"));

				Product special = new Product();
				special.setName("Special Toasted Siopao");
				special.setCategory(siopaoCat);
				special.setPrice(new BigDecimal("15.00"));
				special.setDescription("Our classic toasted siopao with pork and egg filling.");
				special.setCurrentStock(20);
				special.setLowStockThreshold(10);
				special.setRecipeLocked(true);
				special.addIngredient(new RecipeIngredient(special, flour, new BigDecimal("0.05")));
				special.addIngredient(new RecipeIngredient(special, pork, new BigDecimal("0.05")));
				special.addIngredient(new RecipeIngredient(special, egg, new BigDecimal("0.125")));
				specialProduct = productRepo.save(special);

				Product bola = new Product();
				bola.setName("Bola-Bola Siopao");
				bola.setCategory(siopaoCat);
				bola.setPrice(new BigDecimal("20.00"));
				bola.setDescription("Premium meatball filling with salted egg.");
				bola.setCurrentStock(15);
				bola.addIngredient(new RecipeIngredient(bola, flour, new BigDecimal("0.06")));
				bola.addIngredient(new RecipeIngredient(bola, pork, new BigDecimal("0.08")));
				productRepo.save(bola);

				Product coke = new Product();
				coke.setName("Coke Mismo");
				coke.setCategory(drinksCat);
				coke.setPrice(new BigDecimal("25.00"));
				coke.setCurrentStock(50);
				drinksProduct = productRepo.save(coke);

				log.info(">>> Demo Inventory & Products Seeded.");
			} else {
				// If skipping creation, just fetch them for order seeding
				specialProduct = productRepo.findByNameIgnoreCase("Special Toasted Siopao").orElse(null);
				drinksProduct = productRepo.findByNameIgnoreCase("Coke Mismo").orElse(null);
			}

			// 4. Seeding Demo Orders (If none exist)
			if (orderRepo.count() == 0 && specialProduct != null && drinksProduct != null) {
				log.info(">>> Seeding Demo Orders...");

				// Order 1: Completed Last Week
				Order o1 = new Order();
				o1.setUser(customerUser);
				o1.setOrderDate(LocalDateTime.now().minusDays(7));
				o1.setStatus(Order.STATUS_DELIVERED);
				o1.setPaymentMethod("COD");
				o1.setPaymentStatus(Order.PAYMENT_PAID);
				o1.setShippingFirstName("Test");
				o1.setShippingLastName("Customer");
				o1.setShippingAddress("Demo Street");

				o1.addItem(new OrderItem(specialProduct, 2, specialProduct.getPrice())); // 30
				o1.addItem(new OrderItem(drinksProduct, 1, drinksProduct.getPrice())); // 25
				o1.setTotalAmount(new BigDecimal("55.00"));
				orderRepo.save(o1);

				// Order 2: Completed Yesterday
				Order o2 = new Order();
				o2.setUser(customerUser);
				o2.setOrderDate(LocalDateTime.now().minusDays(1));
				o2.setStatus(Order.STATUS_DELIVERED);
				o2.setPaymentMethod("GCASH");
				o2.setPaymentStatus(Order.PAYMENT_PAID);
				o2.setShippingFirstName("Test");
				o2.setShippingLastName("Customer");
				o2.setTransactionId("1234567890123");

				o2.addItem(new OrderItem(specialProduct, 10, specialProduct.getPrice())); // 150
				o2.setTotalAmount(new BigDecimal("150.00"));
				orderRepo.save(o2);

				// Order 3: Pending Verification (Today)
				Order o3 = new Order();
				o3.setUser(customerUser);
				o3.setOrderDate(LocalDateTime.now());
				o3.setStatus(Order.STATUS_PENDING_VERIFICATION);
				o3.setPaymentMethod("GCASH");
				o3.setPaymentStatus(Order.PAYMENT_FOR_VERIFICATION);
				o3.setShippingFirstName("Test");
				o3.setShippingLastName("Customer");
				o3.setTransactionId("9876543210987");

				o3.addItem(new OrderItem(drinksProduct, 5, drinksProduct.getPrice())); // 125
				o3.setTotalAmount(new BigDecimal("125.00"));
				orderRepo.save(o3);

				log.info(">>> Demo Orders Seeded.");
			}
		};
	}
}