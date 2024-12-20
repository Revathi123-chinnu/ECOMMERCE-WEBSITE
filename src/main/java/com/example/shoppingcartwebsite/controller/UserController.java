package com.example.shoppingcartwebsite.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller; // Import the Controller annotation
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.shoppingcartwebsite.model.Cart;
import com.example.shoppingcartwebsite.model.Category;
import com.example.shoppingcartwebsite.model.OrderRequest;
import com.example.shoppingcartwebsite.model.ProductOrder;
import com.example.shoppingcartwebsite.model.UserDtls;
import com.example.shoppingcartwebsite.service.CartService;
import com.example.shoppingcartwebsite.service.CategoryService;
import com.example.shoppingcartwebsite.service.OrderService;
import com.example.shoppingcartwebsite.service.UserService;
import com.example.shoppingcartwebsite.util.CommonUtil;
import com.example.shoppingcartwebsite.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;


@Controller // Add this annotation
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

	@Autowired
	private OrderService orderService;

	@Autowired
    private CommonUtil commonUtil;


    @Autowired
	private CategoryService categoryService;

    @Autowired
    private CartService cartService;

    @GetMapping("/")
    public String showHomePage() {
       // Pass title to the layout
        return "user/home"; // Ensure this matches the correct path

        
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

    @GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {
		Cart saveCart = cartService.saveCart(pid, uid);

		if (ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg", "Product add to cart failed");
		} else {
			session.setAttribute("succMsg", "Product added to cart");
		}
		 return "redirect:/product/" + pid;
        
	}

    private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

    @GetMapping("/cart")
	public String loadCartPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/cart";
	}
    @GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cid) {
		cartService.updateQuantity(sy, cid);
		return "redirect:/user/cart";
	}

    @GetMapping("/orders")
	public String orderPage(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double orderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice() + 250 + 100;
			m.addAttribute("orderPrice", orderPrice);
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/order"; 
	}


		@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p) throws Exception {
		 System.out.println(request);
		UserDtls user = getLoggedInUserDetails(p);
		orderService.saveOrder(user.getId(), request);

		return "user/success";
	}

	@GetMapping("/user-orders")
	public String myOrder(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId());
		m.addAttribute("orders", orders);
		return "/user/my_orders";
	}

	@GetMapping("/update-status")
public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {
    String status = Arrays.stream(OrderStatus.values())
                          .filter(orderSt -> orderSt.getId().equals(st))
                          .map(OrderStatus::getName)
                          .findFirst()
                          .orElse(null);

    ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

    if (!ObjectUtils.isEmpty(updateOrder)) {
        session.setAttribute("statusMessage", "Status Updated");
    } else {
        session.setAttribute("statusMessage", "Status update failed");
    }

    // Consider adding logging instead of printStackTrace in production
    try {
        commonUtil.sendMailForProductOrder(updateOrder, status);
    } catch (Exception e) {
        e.printStackTrace(); // Replace with logger
    }
    return "redirect:/user/user-orders";
}







}
