package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.Holiday;
import ar.com.anepanet.planificator.service.HolidayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidays;

    public HolidayController(HolidayService holidays) {
        this.holidays = holidays;
    }

    @GetMapping
    public List<Holiday> list(@RequestParam int year) {
        return holidays.forYear(year);
    }
}
