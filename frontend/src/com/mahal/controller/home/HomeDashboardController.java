package com.mahal.controller.home;

import com.mahal.database.*;
import com.mahal.model.DueCollection;
import com.mahal.model.Expense;
import com.mahal.model.Income;
import com.mahal.model.IncomeType;
import com.mahal.model.Member;
import com.mahal.util.FormatUtil;
import com.mahal.util.StyleHelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Home dashboard shown before the Masjid page.
 * Uses real amounts and counts from existing DAOs to drive
 * summary cards and a donation trends chart, with filter options.
 * All dates are interpreted in Indian Standard Time via {@link FormatUtil}.
 */
public class HomeDashboardController {

        private final IncomeDAO incomeDAO;
        private final IncomeTypeDAO incomeTypeDAO;
        private final ExpenseDAO expenseDAO;
        private final DueCollectionDAO dueCollectionDAO;
        private final MemberDAO memberDAO;
        private VBox dashboardContent;
        private DatePicker fromDatePicker;
        private DatePicker toDatePicker;
        private ComboBox<String> periodCombo;

        public HomeDashboardController() {
                this.incomeDAO = new IncomeDAO();
                this.incomeTypeDAO = new IncomeTypeDAO();
                this.expenseDAO = new ExpenseDAO();
                this.dueCollectionDAO = new DueCollectionDAO();
                this.memberDAO = new MemberDAO();

                // Subscribe to sync/data events to keep the dashboard fresh
                com.mahal.util.EventBus.getInstance().subscribe("income_types",
                                e -> javafx.application.Platform.runLater(this::refreshDashboard));
                com.mahal.util.EventBus.getInstance().subscribe("incomes",
                                e -> javafx.application.Platform.runLater(this::refreshDashboard));
                com.mahal.util.EventBus.getInstance().subscribe("due_collections",
                                e -> javafx.application.Platform.runLater(this::refreshDashboard));
                com.mahal.util.EventBus.getInstance().subscribe("expenses",
                                e -> javafx.application.Platform.runLater(this::refreshDashboard));
                com.mahal.util.EventBus.getInstance().subscribe("members",
                                e -> javafx.application.Platform.runLater(this::refreshDashboard));
        }

        private void refreshDashboard() {
                if (dashboardContent != null && fromDatePicker != null && toDatePicker != null) {
                        rebuildDashboard(dashboardContent, fromDatePicker.getValue(), toDatePicker.getValue());
                }
        }

        public VBox getView() {
                VBox root = new VBox(20);
                root.setPadding(new Insets(20));
                root.setStyle("-fx-background-color: " + StyleHelper.BG_GRAY_50 + ";");

                LocalDate today = FormatUtil.todayIst();
                YearMonth thisMonth = YearMonth.of(today.getYear(), today.getMonth());
                LocalDate defaultStart = thisMonth.atDay(1);
                LocalDate defaultEnd = thisMonth.atEndOfMonth();

                // Filter Card - Using FlowPane for responsiveness
                FlowPane filterCard = new FlowPane();
                filterCard.setHgap(20);
                filterCard.setVgap(15);
                filterCard.setAlignment(Pos.CENTER_LEFT);
                filterCard.setPadding(new Insets(15, 20, 15, 20));
                filterCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                                "-fx-border-color: #f3f4f6; -fx-border-width: 1; -fx-border-radius: 12;");
                filterCard.setPrefWrapLength(1000); // Allow it to span full width before wrapping

                VBox periodBox = new VBox(5);
                Label periodLabel = new Label("FILTERS");
                periodLabel.setStyle(StyleHelper.getFilterLabelStyle());

                periodCombo = new ComboBox<>();
                periodCombo.getItems().addAll("This Month", "Last Month", "This Year", "Custom");
                periodCombo.setValue("This Month");
                periodCombo.setPrefWidth(130);
                StyleHelper.styleComboBox(periodCombo);

                periodBox.getChildren().addAll(periodLabel, periodCombo);

                fromDatePicker = new DatePicker(defaultStart);
                toDatePicker = new DatePicker(defaultEnd);
                fromDatePicker.setPrefWidth(125);
                toDatePicker.setPrefWidth(125);
                StyleHelper.styleDatePicker(fromDatePicker);
                StyleHelper.styleDatePicker(toDatePicker);

                VBox fromBox = new VBox(5);
                Label fromLbl = new Label("FROM DATE");
                fromLbl.setStyle(StyleHelper.getFilterLabelStyle());
                fromBox.getChildren().addAll(fromLbl, fromDatePicker);

                VBox toBox = new VBox(5);
                Label toLbl = new Label("TO DATE");
                toLbl.setStyle(StyleHelper.getFilterLabelStyle());
                toBox.getChildren().addAll(toLbl, toDatePicker);

                Button applyButton = new Button("Apply Filters");
                applyButton.setStyle(StyleHelper.getApplyFilterButtonStyle());

                Button resetButton = new Button("Reset");
                resetButton.setStyle(StyleHelper.getResetButtonStyle());

                HBox buttons = new HBox(10, applyButton, resetButton);
                buttons.setAlignment(Pos.BOTTOM_LEFT);

                filterCard.getChildren().addAll(periodBox, fromBox, toBox, buttons);

                root.getChildren().addAll(filterCard);

                // Container that will hold the dynamic dashboard content
                dashboardContent = new VBox(25);
                VBox.setVgrow(dashboardContent, Priority.ALWAYS);

                // Initial load
                rebuildDashboard(dashboardContent, defaultStart, defaultEnd);

                // Preset range selection
                periodCombo.setOnAction(e -> {
                        LocalDate start = defaultStart;
                        LocalDate end = defaultEnd;
                        String selected = periodCombo.getValue();
                        if ("Last Month".equals(selected)) {
                                YearMonth last = thisMonth.minusMonths(1);
                                start = last.atDay(1);
                                end = last.atEndOfMonth();
                        } else if ("This Year".equals(selected)) {
                                start = LocalDate.of(today.getYear(), 1, 1);
                                end = LocalDate.of(today.getYear(), 12, 31);
                        }
                        if (!"Custom".equals(selected)) {
                                fromDatePicker.setValue(start);
                                toDatePicker.setValue(end);
                                rebuildDashboard(dashboardContent, start, end);
                        }
                });

                // Apply custom range
                applyButton.setOnAction(e -> {
                        LocalDate start = fromDatePicker.getValue();
                        LocalDate end = toDatePicker.getValue();
                        if (start == null || end == null || end.isBefore(start)) {
                                // Fallback to default month if invalid
                                start = defaultStart;
                                end = defaultEnd;
                                fromDatePicker.setValue(start);
                                toDatePicker.setValue(end);
                        }
                        periodCombo.setValue("Custom");
                        rebuildDashboard(dashboardContent, start, end);
                });

                // Reset
                resetButton.setOnAction(e -> {
                        periodCombo.setValue("This Month");
                        fromDatePicker.setValue(defaultStart);
                        toDatePicker.setValue(defaultEnd);
                        rebuildDashboard(dashboardContent, defaultStart, defaultEnd);
                });

                root.getChildren().add(dashboardContent);
                return root;
        }

        /**
         * Loads data for the given date range and rebuilds the statistic cards and
         * charts.
         */
        private void rebuildDashboard(VBox container, LocalDate startDate, LocalDate endDate) {
                container.getChildren().clear();

                if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
                        LocalDate today = FormatUtil.todayIst();
                        YearMonth month = YearMonth.of(today.getYear(), today.getMonth());
                        startDate = month.atDay(1);
                        endDate = month.atEndOfMonth();
                }

                LocalDate from = startDate;
                LocalDate to = endDate;

                // Load income and map income type -> category (DONATION / etc.)
                // Note: Since type field was removed from UI, it may be null - filter those out
                List<Income> allIncome = incomeDAO.getAll();
                List<IncomeType> allTypes = incomeTypeDAO.getAll();
                Map<Long, String> typeById = allTypes.stream()
                                .filter(it -> it.getId() != null && it.getType() != null) // Filter out null IDs and
                                                                                          // null types
                                .collect(Collectors.toMap(IncomeType::getId, IncomeType::getType, (a, b) -> a));

                List<Income> incomeRange = allIncome.stream()
                                .filter(i -> i.getDate() != null &&
                                                !i.getDate().isBefore(from) &&
                                                !i.getDate().isAfter(to))
                                .toList();

                List<Income> donationIncome = incomeRange.stream()
                                .filter(i -> {
                                        Long typeId = i.getIncomeTypeId();
                                        String t = typeId != null ? typeById.get(typeId) : null;
                                        return t == null || "DONATION".equalsIgnoreCase(t);
                                })
                                .toList();

                List<Expense> allExpenses = expenseDAO.getAll();
                List<Expense> expensesRange = allExpenses.stream()
                                .filter(e -> e.getDate() != null &&
                                                !e.getDate().isBefore(from) &&
                                                !e.getDate().isAfter(to))
                                .toList();

                List<DueCollection> allCollections = dueCollectionDAO.getAll();

                List<Member> members = memberDAO.getAll();

                BigDecimal totalDonations = allIncome.stream()
                                .map(Income::getAmount)
                                .filter(a -> a != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpenses = allExpenses.stream()
                                .map(Expense::getAmount)
                                .filter(a -> a != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalDuesCollected = allCollections.stream()
                                .map(DueCollection::getAmount)
                                .filter(a -> a != null)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal netBalance = totalDonations.add(totalDuesCollected).subtract(totalExpenses);

                // Top summary cards - Using FlowPane for responsiveness (wrapping)
                FlowPane cardsRow = new FlowPane();
                cardsRow.setHgap(20);
                cardsRow.setVgap(20);
                cardsRow.setAlignment(Pos.CENTER_LEFT);

                VBox duesCard = createStatCard(
                                "Due Collections",
                                FormatUtil.formatCurrency(totalDuesCollected),
                                allCollections.size() + " total collections",
                                "#10b981", "💰");

                VBox donationsCard = createStatCard(
                                "Total Donations",
                                FormatUtil.formatCurrency(totalDonations),
                                allIncome.size() + " total records",
                                "#16a34a", "🧧");

                VBox expensesCard = createStatCard(
                                "Total Expenses",
                                FormatUtil.formatCurrency(totalExpenses),
                                allExpenses.size() + " total records",
                                "#ef4444", "📉");

                VBox membersCard = createStatCard(
                                "Registered Members",
                                String.valueOf(members.size()),
                                "Active members",
                                "#2563eb", "👥");

                VBox balanceCard = createStatCard(
                                "Net Balance",
                                FormatUtil.formatCurrency(netBalance),
                                netBalance.compareTo(BigDecimal.ZERO) >= 0 ? "+ Surplus" : "- Deficit",
                                netBalance.compareTo(BigDecimal.ZERO) >= 0 ? "#10b981" : "#ef4444", "⚖️");

                // All statistic cards in a single row
                cardsRow.getChildren().addAll(duesCard, donationsCard, expensesCard, membersCard, balanceCard);

                // Middle row: Donation trends chart
                HBox middleRow = new HBox(20);
                middleRow.setAlignment(Pos.TOP_LEFT);

                VBox donationTrendsContainer = createDonationTrendsChart(donationIncome, from, to);

                middleRow.getChildren().addAll(donationTrendsContainer);
                HBox.setHgrow(donationTrendsContainer, Priority.ALWAYS);

                // Bottom row: Expense trends chart
                HBox expenseRow = new HBox(20);
                expenseRow.setAlignment(Pos.TOP_LEFT);

                VBox expenseTrendsContainer = createExpenseTrendsChart(expensesRange, from, to);

                expenseRow.getChildren().addAll(expenseTrendsContainer);
                HBox.setHgrow(expenseTrendsContainer, Priority.ALWAYS);

                container.getChildren().addAll(cardsRow, middleRow, expenseRow);
        }

        private VBox createStatCard(String title, String value, String desc, String color, String icon) {
                VBox card = new VBox(15);
                card.setPadding(new Insets(20));
                card.setPrefWidth(260);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                                "-fx-border-color: #f3f4f6; -fx-border-width: 1; -fx-border-radius: 12;");

                HBox topRow = new HBox(12);
                topRow.setAlignment(Pos.TOP_LEFT);

                Label iconLabel = new Label(icon);
                iconLabel.setAlignment(Pos.CENTER);
                iconLabel.setPrefSize(44, 44);
                iconLabel.setStyle("-fx-background-color: " + color + "1A; -fx-text-fill: " + color + "; " +
                                "-fx-background-radius: 12; -fx-font-size: 20px;");

                VBox textColumn = new VBox(2);
                Label titleLabel = new Label(title.toUpperCase());
                titleLabel.setStyle(
                                "-fx-text-fill: #6b7280; -fx-font-size: 11px; -fx-font-weight: 700; -fx-letter-spacing: 0.5px;");

                Label valueLabel = new Label(value);
                valueLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 24px; -fx-font-weight: 800;");

                textColumn.getChildren().addAll(titleLabel, valueLabel);
                topRow.getChildren().addAll(iconLabel, textColumn);

                Label descLabel = new Label(desc);
                descLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: 600;");

                card.getChildren().addAll(topRow, descLabel);
                return card;
        }

        private VBox createDonationTrendsChart(
                        List<Income> donations,
                        LocalDate start,
                        LocalDate end) {

                CategoryAxis xAxis = new CategoryAxis();
                NumberAxis yAxis = new NumberAxis();
                AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
                chart.setTitle(null);
                chart.setLegendVisible(false);
                chart.setCreateSymbols(true);
                chart.setHorizontalGridLinesVisible(true);
                chart.setVerticalGridLinesVisible(false);
                chart.setPrefHeight(300);

                // Add a title label manually for better styling
                Label chartTitle = new Label("Donation Trends");
                chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
                Label chartSubTitle = new Label("Total donation volume over time");
                chartSubTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-padding: 0 0 15 0;");

                VBox container = new VBox(2, chartTitle, chartSubTitle, chart);
                container.setPadding(new Insets(20));
                container.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                                "-fx-border-color: #f3f4f6; -fx-border-width: 1; -fx-border-radius: 12;");

                XYChart.Series<String, Number> series = new XYChart.Series<>();

                long daysBetween = ChronoUnit.DAYS.between(start, end);
                if (daysBetween <= 45) {
                        // Group by Day
                        Map<LocalDate, BigDecimal> byDate = donations.stream()
                                        .filter(i -> i.getDate() != null && i.getAmount() != null)
                                        .collect(Collectors.groupingBy(
                                                        Income::getDate,
                                                        TreeMap::new,
                                                        Collectors.mapping(Income::getAmount,
                                                                        Collectors.reducing(BigDecimal.ZERO,
                                                                                        BigDecimal::add))));

                        LocalDate cursor = start;
                        while (!cursor.isAfter(end)) {
                                BigDecimal amount = byDate.getOrDefault(cursor, BigDecimal.ZERO);
                                series.getData().add(new XYChart.Data<>(
                                                String.valueOf(cursor.getDayOfMonth()),
                                                amount));
                                cursor = cursor.plusDays(1);
                        }
                } else {
                        // Group by Month
                        Map<YearMonth, BigDecimal> byMonth = donations.stream()
                                        .filter(i -> i.getDate() != null && i.getAmount() != null)
                                        .collect(Collectors.groupingBy(
                                                        i -> YearMonth.from(i.getDate()),
                                                        TreeMap::new,
                                                        Collectors.mapping(Income::getAmount,
                                                                        Collectors.reducing(BigDecimal.ZERO,
                                                                                        BigDecimal::add))));

                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM");
                        YearMonth cursor = YearMonth.from(start);
                        YearMonth limit = YearMonth.from(end);
                        while (!cursor.isAfter(limit)) {
                                BigDecimal amount = byMonth.getOrDefault(cursor, BigDecimal.ZERO);
                                series.getData().add(new XYChart.Data<>(
                                                cursor.format(fmt),
                                                amount));
                                cursor = cursor.plusMonths(1);
                        }
                }

                chart.getData().add(series);
                // Apply Emerald colors directly to chart
                chart.setStyle("CHART_COLOR_1: #10b981;");
                // In some FX versions, we need to set the series style after adding
                if (series.getNode() != null) {
                        series.getNode().setStyle("-fx-stroke: #10b981; -fx-fill: rgba(16, 185, 129, 0.2);");
                }

                chart.setMinWidth(600);
                return container;
        }

        private VBox createExpenseTrendsChart(
                        List<Expense> expenses,
                        LocalDate start,
                        LocalDate end) {

                CategoryAxis xAxis = new CategoryAxis();
                NumberAxis yAxis = new NumberAxis();
                AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
                chart.setTitle(null);
                chart.setLegendVisible(false);
                chart.setCreateSymbols(true);
                chart.setHorizontalGridLinesVisible(true);
                chart.setVerticalGridLinesVisible(false);
                chart.setPrefHeight(300);

                // Add a title label manually for better styling
                Label chartTitle = new Label("Expense Trends");
                chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
                Label chartSubTitle = new Label("Total expense volume over time");
                chartSubTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-padding: 0 0 15 0;");

                VBox container = new VBox(2, chartTitle, chartSubTitle, chart);
                container.setPadding(new Insets(20));
                container.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); " +
                                "-fx-border-color: #f3f4f6; -fx-border-width: 1; -fx-border-radius: 12;");

                XYChart.Series<String, Number> series = new XYChart.Series<>();

                long daysBetween = ChronoUnit.DAYS.between(start, end);
                if (daysBetween <= 45) {
                        // Group by Day
                        Map<LocalDate, BigDecimal> byDate = expenses.stream()
                                        .filter(e -> e.getDate() != null && e.getAmount() != null)
                                        .collect(Collectors.groupingBy(
                                                        Expense::getDate,
                                                        TreeMap::new,
                                                        Collectors.mapping(Expense::getAmount,
                                                                        Collectors.reducing(BigDecimal.ZERO,
                                                                                        BigDecimal::add))));

                        LocalDate cursor = start;
                        while (!cursor.isAfter(end)) {
                                BigDecimal amount = byDate.getOrDefault(cursor, BigDecimal.ZERO);
                                series.getData().add(new XYChart.Data<>(
                                                String.valueOf(cursor.getDayOfMonth()),
                                                amount));
                                cursor = cursor.plusDays(1);
                        }
                } else {
                        // Group by Month
                        Map<YearMonth, BigDecimal> byMonth = expenses.stream()
                                        .filter(e -> e.getDate() != null && e.getAmount() != null)
                                        .collect(Collectors.groupingBy(
                                                        e -> YearMonth.from(e.getDate()),
                                                        TreeMap::new,
                                                        Collectors.mapping(Expense::getAmount,
                                                                        Collectors.reducing(BigDecimal.ZERO,
                                                                                        BigDecimal::add))));

                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM");
                        YearMonth cursor = YearMonth.from(start);
                        YearMonth limit = YearMonth.from(end);
                        while (!cursor.isAfter(limit)) {
                                BigDecimal amount = byMonth.getOrDefault(cursor, BigDecimal.ZERO);
                                series.getData().add(new XYChart.Data<>(
                                                cursor.format(fmt),
                                                amount));
                                cursor = cursor.plusMonths(1);
                        }
                }

                chart.getData().add(series);
                // Apply Red colors for expenses
                chart.setStyle("CHART_COLOR_1: #ef4444;");

                chart.setMinWidth(600);
                return container;
        }
}
