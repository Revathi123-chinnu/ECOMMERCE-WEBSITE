package com.example.shoppingcartwebsite.service.Impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shoppingcartwebsite.model.Cart;
import com.example.shoppingcartwebsite.model.OrderAddress;
import com.example.shoppingcartwebsite.model.OrderRequest;
import com.example.shoppingcartwebsite.model.ProductOrder;
import com.example.shoppingcartwebsite.repository.CartRepository;
import com.example.shoppingcartwebsite.repository.ProductOrderRepository;
import com.example.shoppingcartwebsite.service.OrderService;
import com.example.shoppingcartwebsite.util.CommonUtil;
import com.example.shoppingcartwebsite.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductOrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CommonUtil commonUtil;

    @Override
    @Transactional
    public void saveOrder(Integer userId, OrderRequest orderRequest) throws Exception {
        // Retrieve the user's cart items
        List<Cart> carts = cartRepository.findByUserId(userId);
        if (carts.isEmpty()) {
            throw new Exception("No items in cart for the user.");
        }

        // Iterate through the cart items to create orders
        for (Cart cart : carts) {
            ProductOrder order = new ProductOrder();
            order.setOrderId(UUID.randomUUID().toString());
            order.setOrderDate(LocalDate.now());
            order.setProduct(cart.getProduct());
            order.setPrice(cart.getProduct().getDiscountPrice());
            order.setQuantity(cart.getQuantity());
            order.setUser(cart.getUser());
            order.setStatus(OrderStatus.IN_PROGRESS.getName());
            order.setPaymentType(orderRequest.getPaymentType());

            // Set the order address details
            OrderAddress address = new OrderAddress();
            address.setFirstName(orderRequest.getFirstName());
            address.setLastName(orderRequest.getLastName());
            address.setEmail(orderRequest.getEmail());
            address.setMobileNo(orderRequest.getMobileNo());
            address.setAddress(orderRequest.getAddress());
            address.setCity(orderRequest.getCity());
            address.setState(orderRequest.getState());
            address.setPincode(orderRequest.getPincode());

            order.setOrderAddress(address);

            // Save the order in the database
            orderRepository.save(order);
            // Uncomment if you want to send an email notification for the order
            // commonUtil.sendMailForProductOrder(order, "success");
        }
    }

    @Override
    public List<ProductOrder> getOrdersByUser(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public ProductOrder updateOrderStatus(Integer id, String status) {
        Optional<ProductOrder> findById = orderRepository.findById(id);
        if (findById.isPresent()) {
            ProductOrder productOrder = findById.get();
            productOrder.setStatus(status);
            return orderRepository.save(productOrder);
        }
        return null;
    }

    @Override
    public List<ProductOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
        if (pageNo < 0 || pageSize <= 0) {
            throw new IllegalArgumentException("Page number and page size must be greater than zero.");
        }
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return orderRepository.findAll(pageable);
    }

    @Override
    public ProductOrder getOrdersByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    @Override
    public boolean cancelOrder(Integer id) {
        // Retrieve the order by ID from the repository
        Optional<ProductOrder> optionalOrder = orderRepository.findById(id);
        
        // Check if the order exists
        if (optionalOrder.isPresent()) {
            ProductOrder order = optionalOrder.get();
            
            // Check if the order is not already cancelled
            if (!"Cancelled".equals(order.getStatus())) {
                // Set the status to "Cancelled"
                order.setStatus("Cancelled");
                
                // Save the updated order back to the repository
                orderRepository.save(order);
                
                // Return true indicating successful cancellation
                return true;
            }
        }
        
        // Return false if the order was not found or was already cancelled
        return false;
    }
}
