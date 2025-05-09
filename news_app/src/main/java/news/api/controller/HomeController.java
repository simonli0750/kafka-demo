package news.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  // Only handle specific paths
  @GetMapping(value = {"/"})
  public String home() {
    return "index.html";  // Return the view name directly, don't forward
  }

  // Handle SPA routes
  @GetMapping(value = {"/{path:[^\\.]*}"})
  public String forwardToHome() {
    return "index.html";  // Return the view name directly, don't forward
  }
}
