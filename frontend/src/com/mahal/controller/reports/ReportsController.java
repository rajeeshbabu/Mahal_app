package com.mahal.controller.reports;

import com.mahal.database.DueCollectionDAO;
import com.mahal.database.ExpenseDAO;
import com.mahal.database.IncomeDAO;
import com.mahal.database.MemberDAO;
import com.mahal.model.DueCollection;
import com.mahal.model.Expense;
import com.mahal.model.Income;
import com.mahal.model.Member;
import com.mahal.util.FormatUtil;
import com.mahal.util.StyleHelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * High‑level dashboard / reports view built on real data from the
 * accounts module (income, expenses, due collections) and members.
 *
 * All dates use IST (via FormatUtil.todayIst) and all currency is
 * displayed as INR.
 */
public class ReportsController {

        private final IncomeDAO incomeDAO;
        private final ExpenseDAO expenseDAO;
        private final DueCollectionDAO dueCollectionDAO;
        private final MemberDAO memberDAO;

        public ReportsController() {
                this.incomeDAO = new IncomeDAO();
                this.expenseDAO = new ExpenseDAO();
                this.dueCollectionDAO = new DueCollectionDAO();
                this.memberDAO = new MemberDAO();
        }

        public javafx.scene.control.ScrollPane getView() {
                VBox root = new VBox(24);
                root.setPadding(new Insets(24));
                root.setStyle("-fx-background-color: " + StyleHelper.BG_GRAY_50 + ";");

                LocalDate today = FormatUtil.todayIst();
                LocalDate defaultStart = today.withDayOfMonth(1);
                LocalDate defaultEnd = today.withDayOfMonth(today.lengthOfMonth());

                // Header Section with Filter Card
                HBox filterRow = new HBox(16);
                filterRow.setAlignment(Pos.CENTER_LEFT);

                Label periodLabel = new Label("Period:");
                periodLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");

                ComboBox<String> periodCombo = new ComboBox<>();
                periodCombo.getStyleClass().add("modern-combo");
                periodCombo.getItems().addAll("This Month", "Last Month", "This Year", "Custom");
                periodCombo.setValue("This Month");
                StyleHelper.styleComboBox(periodCombo);
                periodCombo.setPrefWidth(140);

                Label fromLabel = new Label("From:");
                fromLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");
                DatePicker fromDatePicker = new DatePicker(defaultStart);
                StyleHelper.styleDatePicker(fromDatePicker);
                fromDatePicker.setPrefWidth(150);
                fromDatePicker.setDisable(true);

                Label toLabel = new Label("To:");
                toLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: 600;");
                DatePicker toDatePicker = new DatePicker(defaultEnd);
                StyleHelper.styleDatePicker(toDatePicker);
                toDatePicker.setPrefWidth(150);
                toDatePicker.setDisable(true);

                Button applyBtn = new Button("Apply");
                applyBtn.setStyle(StyleHelper.getApplyFilterButtonStyle());
                applyBtn.setDisable(true);

                Button resetBtn = new Button("Reset");
                resetBtn.setStyle(StyleHelper.getResetButtonStyle());

                filterRow.getChildren().addAll(periodLabel, periodCombo, fromLabel, fromDatePicker, toLabel,
                                toDatePicker, applyBtn, resetBtn);

                VBox filterCard = new VBox(filterRow);
                filterCard.setPadding(new Insets(16, 20, 16, 20));
                filterCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; "
                                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

                VBox contentBox = new VBox(24);

                // Logic
                periodCombo.setOnAction(e -> {
                        boolean isCustom = "Custom".equals(periodCombo.getValue());
                        fromDatePicker.setDisable(!isCustom);
                        toDatePicker.setDisable(!isCustom);
                        applyBtn.setDisable(!isCustom);

                        if (!isCustom) {
                                LocalDate start = defaultStart;
                                LocalDate end = defaultEnd;
                                switch (periodCombo.getValue()) {
                                        case "This Month" -> {
                                                start = today.withDayOfMonth(1);
                                                end = today.withDayOfMonth(today.lengthOfMonth());
                                        }
                                        case "Last Month" -> {
                                                LocalDate firstLastMonth = today.minusMonths(1).withDayOfMonth(1);
                                                start = firstLastMonth;
                                                end = firstLastMonth.withDayOfMonth(firstLastMonth.lengthOfMonth());
                                        }
                                        case "This Year" -> {
                                                start = today.withDayOfYear(1);
                                                end = today.withDayOfYear(today.lengthOfYear());
                                        }
                                }
                                fromDatePicker.setValue(start);
                                toDatePicker.setValue(end);
                                rebuildContent(contentBox, start, end);
                        }
                });

                applyBtn.setOnAction(e -> {
                        LocalDate start = fromDatePicker.getValue();
                        LocalDate end = toDatePicker.getValue();
                        if (start != null && end != null && !start.isAfter(end)) {
                                rebuildContent(contentBox, start, end);
                        }
                });

                resetBtn.setOnAction(e -> {
                        periodCombo.setValue("This Month");
                        fromDatePicker.setValue(defaultStart);
                        toDatePicker.setValue(defaultEnd);
                        rebuildContent(contentBox, defaultStart, defaultEnd);
                });

                root.getChildren().addAll(filterCard, contentBox);
                rebuildContent(contentBox, defaultStart, defaultEnd);

                javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(root);
                scroll.setFitToWidth(true);
                scroll.setStyle("-fx-background-color: transparent; -fx-background: " + StyleHelper.BG_GRAY_50 + ";");
                return scroll;
        }

        private void rebuildContent(VBox container, LocalDate start, LocalDate end) {
                container.getChildren().clear();

                List<Income> allIncome = incomeDAO.getAll();
                List<Expense> allExpenses = expenseDAO.getAll();
                List<DueCollection> allCollections = dueCollectionDAO.getAll();
                List<Member> allMembers = memberDAO.getAll();

                List<Income> rangeIncome = allIncome.stream()
                                .filter(i -> i.getDate() != null && !i.getDate().isBefore(start)
                                                && !i.getDate().isAfter(end))
                                .toList();
                List<Expense> rangeExpenses = allExpenses.stream()
                                .filter(e -> e.getDate() != null && !e.getDate().isBefore(start)
                                                && !e.getDate().isAfter(end))
                                .toList();
                List<DueCollection> rangeCollections = allCollections.stream()
                                .filter(c -> c.getDate() != null && !c.getDate().isBefore(start)
                                                && !c.getDate().isAfter(end))
                                .toList();

                BigDecimal totalIncome = rangeIncome.stream().map(Income::getAmount).filter(a -> a != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalExpense = rangeExpenses.stream().map(Expense::getAmount).filter(a -> a != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalCollections = rangeCollections.stream().map(DueCollection::getAmount)
                                .filter(a -> a != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal netBalance = totalIncome.add(totalCollections).subtract(totalExpense);

                // Cards Row
                HBox cardsRow = new HBox(20);
                cardsRow.getChildren().addAll(
                                createStatCard("Total Income", FormatUtil.formatCurrency(totalIncome),
                                                "Income in range"),
                                createStatCard("Due Collections", FormatUtil.formatCurrency(totalCollections),
                                                "Dues in range"),
                                createStatCard("Expenses", FormatUtil.formatCurrency(totalExpense),
                                                "Expenses in range"),
                                createStatCard("Net Balance", FormatUtil.formatCurrency(netBalance),
                                                netBalance.compareTo(BigDecimal.ZERO) >= 0 ? "Surplus" : "Deficit"),
                                createStatCard("Registered Members", String.valueOf(allMembers.size()),
                                                "Total members"));
                cardsRow.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

                // Charts Section
                HBox chartRow = new HBox(24);
                chartRow.setAlignment(Pos.TOP_LEFT);

                VBox barChartCard = wrapInCard(createIncomeExpenseBarChart(rangeIncome, rangeExpenses, start, end),
                                "Income vs Expense (This Month)");
                VBox pieChartCard = wrapInCard(createCollectionByTypePieChart(allCollections.stream()
                                .filter(c -> c.getDate() != null && !c.getDate().isBefore(start)
                                                && !c.getDate().isAfter(end))
                                .toList()), "Due Collections by Type (This Month)");

                HBox.setHgrow(barChartCard, Priority.ALWAYS);
                HBox.setHgrow(pieChartCard, Priority.ALWAYS);
                chartRow.getChildren().addAll(barChartCard, pieChartCard);

                container.getChildren().addAll(cardsRow, chartRow);
        }

        private VBox wrapInCard(javafx.scene.Node content, String title) {
                VBox card = new VBox(16);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
                card.setPadding(new Insets(24));

                Label titleLabel = new Label(title);
                titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");

                card.getChildren().addAll(titleLabel, content);
                return card;
        }

        private VBox createStatCard(String title, String value, String description) {
                VBox card = new VBox(8);
                card.setPadding(new Insets(24));
                card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

                Label lblTitle = new Label(title);
                lblTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: 500;");

                Label lblValue = new Label(value);
                lblValue.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 26px; -fx-font-weight: 800;");

                Label lblDesc = new Label(description);
                lblDesc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                card.getChildren().addAll(lblTitle, lblValue, lblDesc);
                return card;
        }

        private BarChart<String, Number> createIncomeExpenseBarChart(
                        List<Income> incomes,
                        List<Expense> expenses,
                        LocalDate start,
                        LocalDate end) {

                CategoryAxis xAxis = new CategoryAxis();
                xAxis.setLabel("Date");
                NumberAxis yAxis = new NumberAxis();
                yAxis.setLabel("Amount (INR)");

                BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
                chart.setLegendVisible(true);
                chart.setCategoryGap(20);
                chart.setBarGap(4);
                chart.setStyle("-fx-background-color: transparent;");
                chart.setLegendSide(javafx.geometry.Side.BOTTOM);

                XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
                incomeSeries.setName("Income");

                XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
                expenseSeries.setName("Expense");

                // Group sums by date
                Map<LocalDate, BigDecimal> incomeByDate = incomes.stream()
                                .filter(i -> i.getDate() != null && i.getAmount() != null)
                                .collect(Collectors.groupingBy(
                                                Income::getDate,
                                                Collectors.mapping(Income::getAmount,
                                                                Collectors.reducing(BigDecimal.ZERO,
                                                                                BigDecimal::add))));

                Map<LocalDate, BigDecimal> expenseByDate = expenses.stream()
                                .filter(e -> e.getDate() != null && e.getAmount() != null)
                                .collect(Collectors.groupingBy(
                                                Expense::getDate,
                                                Collectors.mapping(Expense::getAmount,
                                                                Collectors.reducing(BigDecimal.ZERO,
                                                                                BigDecimal::add))));

                LocalDate cursor = start;
                while (!cursor.isAfter(end)) {
                        String label = cursor.getDayOfMonth() + "";
                        BigDecimal inc = incomeByDate.getOrDefault(cursor, BigDecimal.ZERO);
                        BigDecimal exp = expenseByDate.getOrDefault(cursor, BigDecimal.ZERO);

                        incomeSeries.getData().add(new XYChart.Data<>(label, inc));
                        expenseSeries.getData().add(new XYChart.Data<>(label, exp));

                        cursor = cursor.plusDays(1);
                }

                chart.getData().addAll(incomeSeries, expenseSeries);
                chart.setMinWidth(500);
                chart.setPrefHeight(350);
                return chart;
        }

        private PieChart createCollectionByTypePieChart(List<DueCollection> collections) {
                PieChart pie = new PieChart();
                pie.setLegendVisible(true);
                pie.setLegendSide(javafx.geometry.Side.BOTTOM);
                pie.setLabelsVisible(true);
                pie.setStyle("-fx-background-color: transparent;");

                Map<String, BigDecimal> byType = collections.stream()
                                .filter(c -> c.getDueTypeName() != null && c.getAmount() != null)
                                .collect(Collectors.groupingBy(
                                                DueCollection::getDueTypeName,
                                                Collectors.mapping(DueCollection::getAmount,
                                                                Collectors.reducing(BigDecimal.ZERO,
                                                                                BigDecimal::add))));

                for (Map.Entry<String, BigDecimal> entry : byType.entrySet()) {
                        String label = entry.getKey() + " (" + FormatUtil.formatCurrency(entry.getValue()) + ")";
                        PieChart.Data slice = new PieChart.Data(label, entry.getValue().doubleValue());
                        pie.getData().add(slice);
                }

                pie.setMinWidth(400);
                pie.setPrefHeight(350);
                return pie;
        }
}
