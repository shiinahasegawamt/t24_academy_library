package jp.co.metateam.library.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.AccountDto;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.BookMstDto;
import jp.co.metateam.library.service.AccountService;
import jp.co.metateam.library.values.AuthorizationTypes;
import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.service.RentalManageService;
import jp.co.metateam.library.values.RentalStatus;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.model.StockDto;
import jp.co.metateam.library.service.StockService;

import lombok.extern.log4j.Log4j2;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.Data;

/**
 * 貸出管理関連クラスß
 */
@Log4j2
@Controller
public class RentalManageController {

    private final AccountService accountService;
    private final RentalManageService rentalManageService;
    private final StockService stockService;

    @Autowired
    public RentalManageController(
            AccountService accountService,
            RentalManageService rentalManageService,
            StockService stockService) {
        this.accountService = accountService;
        this.rentalManageService = rentalManageService;
        this.stockService = stockService;
    }

    /**
     * 貸出一覧画面初期表示
     * 
     * @param model
     * @return
     */
    @GetMapping("/rental/index")
    public String index(Model model) {
        // 貸出管理テーブルから全件取得
        List<RentalManage> rentalManageList = this.rentalManageService.findAll();
        // 貸出一覧画面に渡すデータをmodelに追加
        model.addAttribute("rentalManageList", rentalManageList);
        // 貸出一覧画面に遷移
        return "rental/index";
    }

    @GetMapping("/rental/add")
    public String add(Model model) {
        // テーブルから情報を持ってくる

        List<Stock> stockList = this.stockService.findStockAvailableAll();
        List<Account> accountList = this.accountService.findAll();

        // モデル
        model.addAttribute("rentalStatus", RentalStatus.values());
        model.addAttribute("stockList", stockList);
        model.addAttribute("accounts", accountList);

        if (!model.containsAttribute("rentalManageDto")) {
            model.addAttribute("rentalManageDto", new RentalManageDto());
        }

        return "rental/add";
    }

    @PostMapping("/rental/add")
    public String save(@Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result,
            RedirectAttributes ra) {
        try {
            String rentaladdDataError = this.rentaladdCheck(rentalManageDto, rentalManageDto.getStockId());

            if (rentaladdDataError != null) {
                result.addError(new FieldError("rentalManageDto", "expectedRentalOn", rentaladdDataError));
            }
            if (rentaladdDataError != null) {
                result.addError(new FieldError("rentalManageDto", "expectedReturnOn", rentaladdDataError));
            }
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 登録処理
            this.rentalManageService.save(rentalManageDto);

            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);

            return "rental/add";
        }
    }

    @GetMapping("/rental/{id}/edit")
    public String edit(@PathVariable("id") String id, Model model) {
        List<Stock> stockList = this.stockService.findStockAvailableAll();
        List<Account> accountList = this.accountService.findAll();

        // モデル
        model.addAttribute("rentalStatus", RentalStatus.values());
        model.addAttribute("stockList", stockList);
        model.addAttribute("accounts", accountList);

        if (!model.containsAttribute("rentalManageDto")) {
            RentalManageDto rentalManageDto = new RentalManageDto();
            RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));

            model.addAttribute("rentalManageList", rentalManage);

            rentalManageDto.setId(rentalManage.getId());
            rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());
            rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
            rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());
            rentalManageDto.setStatus(rentalManage.getStatus());
            rentalManageDto.setStockId(rentalManage.getStock().getId());

            model.addAttribute("rentalManageDto", rentalManageDto);

        }

        return "rental/edit";
    }

    @PostMapping("/rental/{id}/edit")
    public String update(@PathVariable("id") String id, @Valid @ModelAttribute RentalManageDto rentalManageDto,
            BindingResult result, RedirectAttributes ra, Model model) {
        try {

            RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));
            String validerror = rentalManageDto.isvalidStatus(rentalManage.getStatus());
            String rentalDataError = this.rentalCheck(rentalManageDto, rentalManage.getStock().getId(),
                    Long.valueOf(id));

            if (validerror != null) {
                result.addError(new FieldError("rentalManageDto", "status", validerror));
            }
            if (rentalDataError != null) {
                result.addError(new FieldError("rentalManageDto", "expectedRentalOn", rentalDataError));
            }
            if (rentalDataError != null) {
                result.addError(new FieldError("rentalManageDto", "expectedReturnOn", rentalDataError));
            }
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }

            // 登録処理
            rentalManageService.update(Long.valueOf(id), rentalManageDto);

            return "redirect:/rental/index";

        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);

            List<Stock> stockList = this.stockService.findStockAvailableAll();
            List<Account> accountList = this.accountService.findAll();

            // モデル
            model.addAttribute("rentalStatus", RentalStatus.values());
            model.addAttribute("stockList", stockList);
            model.addAttribute("accounts", accountList);
            return "rental/edit";
        }
    }

   
    public String rentalCheck(RentalManageDto rentalManageDto, String Id, Long rentalId) {

        List<RentalManage> rentalAvailable = this.rentalManageService.findByStockIdAndStatusIn(Id,
                Long.valueOf(rentalId));

        if (rentalAvailable != null) {

            for (RentalManage list : rentalAvailable) {
                if (list.getExpectedReturnOn().after(rentalManageDto.getExpectedRentalOn())
                        && list.getExpectedRentalOn().before(rentalManageDto.getExpectedReturnOn())) {
                    return "この書籍は、入力された日付で登録できません";
                }
            }
        }
        return null;
    }

   
    public String rentaladdCheck(RentalManageDto rentalManageDto, String Id) {

        List<RentalManage> rentaladdAvailable = this.rentalManageService.findByStockIdAndStatusIn(Id);

        if (rentaladdAvailable != null) {

            for (RentalManage list : rentaladdAvailable) {
                if (list.getExpectedReturnOn().after(rentalManageDto.getExpectedRentalOn())
                        && list.getExpectedRentalOn().before(rentalManageDto.getExpectedReturnOn())) {
                    return "この書籍は、入力された日付で登録できません";
                }
            }
        }
        return null;

    }
}
