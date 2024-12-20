package com.example.shoppingcartwebsite.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.shoppingcartwebsite.model.Category;
import com.example.shoppingcartwebsite.model.Product;
import com.example.shoppingcartwebsite.model.UserDtls;
import com.example.shoppingcartwebsite.service.CartService;
import com.example.shoppingcartwebsite.service.CategoryService;
import com.example.shoppingcartwebsite.service.ProductService;
import com.example.shoppingcartwebsite.service.UserService;
import com.example.shoppingcartwebsite.util.CommonUtil;

import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;


@Controller

public class HomeController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    private final UserService userService;

	@Autowired
    private CommonUtil commonUtil;

	@Autowired
	private CartService cartService;

    @Autowired
    public HomeController(UserService userService) {
        this.userService = userService;
    }
    

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			System.out.println("Cart count for user " + userDtls.getId() + ": " + countCart);
			m.addAttribute("countCart", countCart);
		}
		

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

    @GetMapping("/") 
    public String index() {
        return "index";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/signin")
    public String signin() {
        return "login";
    }

   	@GetMapping("/products")
	public String products(Model m, @RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize,
			@RequestParam(defaultValue = "") String ch) {

		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("paramValue", category);
		m.addAttribute("categories", categories);

//		List<Product> products = productService.getAllActiveProducts(category);
//		m.addAttribute("products", products);
		Page<Product> page = null;
		if (StringUtils.isEmpty(ch)) {
			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
		} else {
			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
		}

		List<Product> products = page.getContent();
		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "product";
	}

    @GetMapping("/product/{id}")
	public String product(@PathVariable int id, Model m) {
		Product productById = productService.getProductById(id);
		m.addAttribute("product", productById);
		return "view_product";
	}

    @GetMapping("/product1")
    public String product1() {
        return "view_product1";
    }

    @GetMapping("/product2")
    public String product2() {
        return "view_product2";
    }

    @PostMapping("/saveUser")
    public String saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
            throws IOException {

            String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
            user.setProfileImage(imageName);
            UserDtls savedUser = userService.saveUser(user);

            if (!ObjectUtils.isEmpty(savedUser)) {
                if (!file.isEmpty()) {
                    // Save the file to the 'static/img/profile_img' directory
                    File saveDir = new ClassPathResource("static/img/profile_img").getFile();

                    Path path = Paths.get(saveDir.getAbsolutePath(), imageName);
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                }
                session.setAttribute("succMsg", "Registered successfully");
            } else {
                session.setAttribute("errorMsg", "Something went wrong on the server");
            }
        

        return "redirect:/register";
    }


    // forgot password logic code

    @GetMapping("/forgot-password")
	public String showForgotPassword() {
		return "forgot_password.html";
	}

	
	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam String email, HttpSession session, HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {

		UserDtls userByEmail = userService.getUserByEmail(email);

		if (ObjectUtils.isEmpty(userByEmail)) {
			session.setAttribute("errorMsg", "Invalid email");
		} else {

			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email, resetToken);

			// Generate URL :
			// http://localhost:8080/reset-password?token=sfgdbgfswegfbdgfewgvsrg

			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

			Boolean sendMail = commonUtil.sendMail(url, email);

			if (sendMail) {
				session.setAttribute("succMsg", "Please check your email..Password Reset link sent");
			} else {
				session.setAttribute("errorMsg", "Somethong wrong on server ! Email not send");
			}
		}

		return "redirect:/forgot-password";
	}

	@GetMapping("/reset-password")
	public String showResetPassword(@RequestParam String token, HttpSession session, Model m) {

		UserDtls userByToken = userService.getUserByToken(token);

		if (userByToken == null) {
			m.addAttribute("msg", "Your link is invalid or expired !!");
			return "message";
		}
		m.addAttribute("token", token);
		return "reset_password";
	}

    

    
 }

