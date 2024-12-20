package com.example.shoppingcartwebsite.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.springframework.data.domain.Page;
import java.security.Principal;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.shoppingcartwebsite.model.Category;
import com.example.shoppingcartwebsite.model.Product;
import com.example.shoppingcartwebsite.model.UserDtls;
import com.example.shoppingcartwebsite.service.CartService;
import com.example.shoppingcartwebsite.service.CategoryService;
import com.example.shoppingcartwebsite.service.ProductService;
import com.example.shoppingcartwebsite.service.UserService;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService; // Corrected naming to follow conventions


    @Autowired
	private UserService userService; 

    @Autowired
    private CartService cartService;

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
        return "admin/index";
    }
   
 
    
    
   
    @GetMapping("/add_product")
    public String loadAddProduct() {
        return "admin/add_product";
    }

    @GetMapping("/loadAddProduct")
    public String loadAddProduct(Model m) {
        List<Category> categories = categoryService.getAllCategory();
        m.addAttribute("categories", categories);
        return "admin/add_product";
    }

    @GetMapping("/category")
    public String category(Model model) {
        model.addAttribute("categories", categoryService.getAllCategory());
        return "admin/category";
    }

    @PostMapping("/saveCategory")
    public String saveCategory(@ModelAttribute Category category, 
                               @RequestParam("file") MultipartFile file, 
                               RedirectAttributes redirectAttributes) {
        String imageName = "default.jpg";
        if (file != null && !file.isEmpty()) {
            imageName = file.getOriginalFilename();
        }
        category.setImageName(imageName);

        Boolean existCategory = categoryService.existCategory(category.getName());
        if (existCategory) {
            redirectAttributes.addFlashAttribute("errorMsg", "Category Name already exists");
            return "redirect:/admin/category";
        }

        Category savedCategory = categoryService.saveCategory(category);
        if (ObjectUtils.isEmpty(savedCategory)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Category not saved due to an internal server error");
            return "redirect:/admin/category";
        }

        if (file != null && !file.isEmpty()) {
            try {
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img"
                        + File.separator + imageName);

                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                redirectAttributes.addFlashAttribute("succMsg", "Category saved successfully");
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Image upload failed: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("succMsg", "Category saved successfully without an image");
        }

        return "redirect:/admin/category";
    }

    @GetMapping("/deleteCategory/{id}")
    public String deleteCategory(@PathVariable int id, HttpSession session) {
        Boolean deleteCategory = categoryService.deleteCategory(id);

        if (deleteCategory) {
            session.setAttribute("succMsg", "Category deleted successfully");
        } else {
            session.setAttribute("errorMsg", "Something went wrong on the server");
        }

        return "redirect:/admin/category";
    }

    @GetMapping("/loadEditCategory/{id}")
    public String loadEditCategory(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Category not found");
            return "redirect:/admin/category";
        }
        model.addAttribute("categories", category);
        return "admin/edit_category";
    }

    @PostMapping("/saveProduct")
public String saveProduct(@ModelAttribute Product product, 
                          @RequestParam("file") MultipartFile image,
                          RedirectAttributes redirectAttributes) throws IOException {

    String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();
    product.setImage(imageName);
    product.setDiscount(0);
    product.setDiscountPrice(product.getPrice());
    
    Product saveProduct = productService.saveProduct(product); // Save product using your service

    // Create directory if it doesn't exist
    File saveFile = new ClassPathResource("static/img").getFile();
    Path productImagePath = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img");

    if (!Files.exists(productImagePath)) {
        Files.createDirectories(productImagePath); // Create the directory if it doesn't exist
    }

    if (saveProduct != null) {
        // Save the uploaded file
        Path path = productImagePath.resolve(image.getOriginalFilename());
        Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        redirectAttributes.addFlashAttribute("succMsg", "Product added successfully!");
    } else {
        redirectAttributes.addFlashAttribute("errorMsg", "Something went wrong on the server.");
    }

    return "redirect:/admin/loadAddProduct"; // Redirecting to the add product page
}

    @PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session, Model m) {

		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			session.setAttribute("errorMsg", "invalid Discount");
		} else {
			Product updateProduct = productService.updateProduct(product, image);
			if (!ObjectUtils.isEmpty(updateProduct)) {
				session.setAttribute("succMsg", "Product update success");
			} else {
				session.setAttribute("errorMsg", "Something wrong on server");
			}
		}
		return "redirect:/admin/editProduct/" + product.getId();
	}






    // view_products

    @GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {
			page = productService.searchProductPagination(pageNo, pageSize, ch);
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
		}
		m.addAttribute("products", page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/products";
	}





    @GetMapping("/deleteProduct/{id}")
    public String deleteProduct(@PathVariable int id, HttpSession session) { // Change int to Long
        Boolean deleteProduct = productService.deleteProduct(id); // Ensure the service method accepts Long
        if (deleteProduct) {
            session.setAttribute("succMsg", "Product delete success");
        } else {
            session.setAttribute("errorMsg", "Something wrong on server");
        }
        return "redirect:/admin/products";
    }
    
    @GetMapping("/editProduct/{id}")
    public String editProduct(@PathVariable int id, Model m) { // Change int to Long
        m.addAttribute("product", productService.getProductById(id)); // Ensure the service method accepts Long
        m.addAttribute("categories", categoryService.getAllCategory());
        return "admin/edit_product";
    }

    

    // userd

    @GetMapping("/users")
    public String getAllUsers(Model m, @RequestParam Integer type) {
        List<UserDtls> users;
        if (type == 1) {
            users = userService.getUsers("ROLE_USER");
        } else {
            users = userService.getUsers("ROLE_ADMIN");
        }
        m.addAttribute("userType", type);
        m.addAttribute("users", users);
        return "/admin/users";
    }
    @GetMapping("/updateSts")
	public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id,@RequestParam Integer type, HttpSession session) {
		Boolean f = userService.updateAccountStatus(id, status);
		if (f) {
			session.setAttribute("succMsg", "Account Status Updated");
		} else {
			session.setAttribute("errorMsg", "Something wrong on server");
		}
		return "redirect:/admin/users?type="+type;
	}

    @GetMapping("/add-admin")
	public String loadAdminAdd() {
		return "/admin/add_admin";
	}

	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
		user.setProfileImage(imageName);
		UserDtls saveUser = userService.saveAdmin(user);

		if (!ObjectUtils.isEmpty(saveUser)) {
			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
						+ file.getOriginalFilename());

//				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Register successfully");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/add-admin";
	}
    @GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
	}

} 