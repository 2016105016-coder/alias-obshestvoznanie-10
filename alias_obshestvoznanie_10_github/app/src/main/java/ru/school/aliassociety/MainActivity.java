package ru.school.aliassociety;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int EASY = 0;
    private static final int MEDIUM = 1;
    private static final int HARD = 2;
    private static final int MIXED = 3;

    private final Random random = new Random();
    private final ArrayList<Card> allCards = new ArrayList<>();
    private final ArrayList<Card> roundDeck = new ArrayList<>();
    private final ArrayList<String> teamNames = new ArrayList<>();
    private final ArrayList<EditText> teamInputs = new ArrayList<>();
    private final List<String> teamNameOptions = GameData.buildTeamNameOptions();

    private int[] scores = new int[]{0, 0, 0};
    private int currentTeam = 0;
    private int difficulty = MEDIUM;
    private int winTarget = 20;
    private int cardIndex = 0;
    private int timeLeft = 70;
    private int roundPoints = 0;
    private boolean soundEnabled = true;
    private boolean roundStarted = false;
    private CountDownTimer timer;

    private TextView timerText;
    private TextView cardText;
    private TextView topicText;
    private TextView teamText;
    private TextView roundInfoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(76, 29, 149));
        window.setNavigationBarColor(Color.rgb(15, 23, 42));
        allCards.addAll(DeckFactory.buildCards());
        initTeams();
        showMainMenu();
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        stopTimer();
        showMainMenu();
    }

    private void initTeams() {
        teamNames.clear();
        teamNames.add("Громкие Мыслители");
        teamNames.add("Молниеносные Знатоки");
        teamNames.add("Неоновые Ораторы");
        scores = new int[]{0, 0, 0};
    }

    private void showMainMenu() {
        stopTimer();
        teamInputs.clear();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(diagonal(new int[]{Color.rgb(76, 29, 149), Color.rgb(217, 70, 239), Color.rgb(249, 115, 22)}, 0));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout content = column();
        content.setPadding(dp(16), dp(18), dp(16), dp(28));
        root.addView(content, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView chip = label("✨ 10 класс · обществознание · " + allCards.size() + " понятий", 13, Color.WHITE, true);
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        chip.setBackground(round(Color.argb(45, 255, 255, 255), dp(18)));
        top.addView(chip, new LinearLayout.LayoutParams(0, -2, 1));
        TextView sound = button3d(soundEnabled ? "🔊" : "🔇", Color.argb(55, 255, 255, 255), Color.WHITE, 19);
        sound.setOnClickListener(v -> {
            soundEnabled = !soundEnabled;
            playSfx("click");
            showMainMenu();
        });
        top.addView(sound, new LinearLayout.LayoutParams(dp(58), dp(52)));
        content.addView(top);

        TextView title = label("Элиас:\nобщество", 52, Color.WHITE, true);
        title.setGravity(Gravity.LEFT);
        title.setShadowLayer(8, 0, 8, Color.argb(80, 0, 0, 0));
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = label("На карточке показывается только понятие. Объясняйте без однокоренных слов, набирайте очки и открывайте победный экран.", 17, Color.argb(230, 255, 255, 255), false);
        subtitle.setPadding(0, dp(10), 0, dp(12));
        content.addView(subtitle);

        LinearLayout stats = row();
        stats.addView(statBox("👥", String.valueOf(teamNames.size()), "команды"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(statBox("📚", "1000", "понятий"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(statBox("🎯", String.valueOf(winTarget), "до победы"), new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(stats);

        TextView start = button3d("▶ Начать раунд", Color.rgb(15, 23, 42), Color.WHITE, 22);
        start.setOnClickListener(v -> {
            updateTeamNamesFromInputs();
            playSfx("start");
            prepareRound();
            showGameScreen();
        });
        content.addView(start, paramsTop(-1, dp(66), 18));

        TextView rules = button3d("Правила и режим занятия", Color.argb(55, 255, 255, 255), Color.WHITE, 16);
        rules.setOnClickListener(v -> {
            playSfx("modal");
            showRules();
        });
        content.addView(rules, paramsTop(-1, dp(58), 10));

        TextView randomSound = button3d("🎵 Проверить случайный звук", Color.rgb(253, 224, 71), Color.rgb(15, 23, 42), 16);
        randomSound.setOnClickListener(v -> playSfx(GameData.SOUND_TYPES[random.nextInt(GameData.SOUND_TYPES.length)]));
        content.addView(randomSound, paramsTop(-1, dp(58), 10));

        content.addView(settingsPanel(), paramsTop(-1, -2, 18));
        content.addView(winTargetPanel(), paramsTop(-1, -2, 14));
        content.addView(teamPanel(), paramsTop(-1, -2, 14));
        content.addView(testPanel(), paramsTop(-1, -2, 14));

        setContentView(scroll);
    }

    private View settingsPanel() {
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(Color.rgb(15, 23, 42), dp(28)));
        panel.addView(label("⚙ Настройки уровня", 23, Color.WHITE, true));

        String[] names = {"Лёгкий", "Средний", "Сложный", "Микс 1000"};
        String[] subs = {"базовые понятия", "курс 10 класса", "научные термины", "весь словарь"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView b = button3d(names[i] + " — " + subs[i], i == difficulty ? Color.WHITE : Color.argb(45, 255, 255, 255), i == difficulty ? Color.rgb(15, 23, 42) : Color.WHITE, 16);
            b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v -> {
                difficulty = index;
                playSfx("level");
                showMainMenu();
            });
            panel.addView(b, paramsTop(-1, dp(58), 10));
        }
        return panel;
    }

    private View winTargetPanel() {
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(Color.argb(45, 255, 255, 255), dp(28)));
        panel.addView(label("🎯 Сколько слов нужно угадать", 20, Color.WHITE, true));

        LinearLayout line = row();
        TextView minus = button3d("−", Color.argb(55, 255, 255, 255), Color.WHITE, 24);
        TextView plus = button3d("+", Color.WHITE, Color.rgb(15, 23, 42), 24);
        EditText input = new EditText(this);
        input.setText(String.valueOf(winTarget));
        input.setGravity(Gravity.CENTER);
        input.setTextColor(Color.rgb(15, 23, 42));
        input.setTextSize(28);
        input.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        input.setBackground(round(Color.WHITE, dp(20)));
        input.setOnEditorActionListener((v, actionId, event) -> {
            winTarget = DeckFactory.clampWinTarget(input.getText().toString());
            input.setText(String.valueOf(winTarget));
            playSfx("target");
            return false;
        });
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                winTarget = DeckFactory.clampWinTarget(input.getText().toString());
                input.setText(String.valueOf(winTarget));
            }
        });
        minus.setOnClickListener(v -> {
            winTarget = DeckFactory.clampWinTarget(winTarget - 1);
            playSfx("target");
            showMainMenu();
        });
        plus.setOnClickListener(v -> {
            winTarget = DeckFactory.clampWinTarget(winTarget + 1);
            playSfx("target");
            showMainMenu();
        });
        line.addView(minus, new LinearLayout.LayoutParams(dp(62), dp(60)));
        line.addView(input, new LinearLayout.LayoutParams(0, dp(60), 1));
        line.addView(plus, new LinearLayout.LayoutParams(dp(62), dp(60)));
        contentSpacing(line, dp(8));
        panel.addView(line, paramsTop(-1, -2, 12));
        TextView hint = label("Победа появится автоматически, когда команда наберёт указанное количество очков. Диапазон: 3–100.", 13, Color.argb(210, 255, 255, 255), false);
        panel.addView(hint, paramsTop(-1, -2, 10));
        return panel;
    }

    private View teamPanel() {
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(Color.argb(45, 255, 255, 255), dp(28)));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(label("👥 Команды · " + teamNameOptions.size() + " вариантов", 20, Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView dice = button3d("🎲", Color.rgb(253, 224, 71), Color.rgb(15, 23, 42), 18);
        dice.setOnClickListener(v -> {
            Collections.shuffle(teamNameOptions);
            for (int i = 0; i < teamNames.size(); i++) teamNames.set(i, teamNameOptions.get(i));
            playSfx("team");
            showMainMenu();
        });
        TextView minus = button3d("−", Color.argb(55, 255, 255, 255), Color.WHITE, 20);
        minus.setOnClickListener(v -> {
            if (teamNames.size() > 2) {
                teamNames.remove(teamNames.size() - 1);
                resizeScores();
                playSfx("team");
                showMainMenu();
            }
        });
        TextView plus = button3d("+", Color.WHITE, Color.rgb(15, 23, 42), 20);
        plus.setOnClickListener(v -> {
            if (teamNames.size() < 8) {
                teamNames.add(teamNameOptions.get(teamNames.size() % teamNameOptions.size()));
                resizeScores();
                playSfx("team");
                showMainMenu();
            }
        });
        header.addView(dice, new LinearLayout.LayoutParams(dp(54), dp(48)));
        header.addView(minus, new LinearLayout.LayoutParams(dp(54), dp(48)));
        header.addView(plus, new LinearLayout.LayoutParams(dp(54), dp(48)));
        contentSpacing(header, dp(6));
        panel.addView(header);

        for (int i = 0; i < teamNames.size(); i++) {
            TextView caption = label("Команда " + (i + 1), 12, Color.argb(180, 255, 255, 255), true);
            panel.addView(caption, paramsTop(-1, -2, 12));
            EditText input = new EditText(this);
            input.setText(teamNames.get(i));
            input.setSingleLine(true);
            input.setTextSize(15);
            input.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            input.setTextColor(Color.rgb(15, 23, 42));
            input.setPadding(dp(14), 0, dp(14), 0);
            input.setBackground(round(Color.WHITE, dp(15)));
            teamInputs.add(input);
            panel.addView(input, new LinearLayout.LayoutParams(-1, dp(52)));
        }
        return panel;
    }

    private View testPanel() {
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(Color.argb(45, 255, 255, 255), dp(28)));
        boolean cardsOk = allCards.size() == 1000;
        boolean namesOk = teamNameOptions.size() > 100;
        boolean soundsOk = GameData.soundVariantCount() > 100;
        boolean noHints = true;
        for (Card c : allCards) {
            String low = c.text.toLowerCase(Locale.ROOT);
            if (low.contains(":") || low.contains("пример из жизни") || low.contains("признаки")) {
                noHints = false;
                break;
            }
        }
        int passed = (cardsOk ? 1 : 0) + (namesOk ? 1 : 0) + (soundsOk ? 1 : 0) + (noHints ? 1 : 0);
        panel.addView(label("🧪 Самопроверка логики: " + passed + "/4", 18, Color.WHITE, true));
        panel.addView(label((cardsOk ? "✅" : "❌") + " карточек: " + allCards.size(), 14, Color.WHITE, false));
        panel.addView(label((noHints ? "✅" : "❌") + " на карточках только понятия, без подсказок", 14, Color.WHITE, false));
        panel.addView(label((namesOk ? "✅" : "❌") + " вариантов команд: " + teamNameOptions.size(), 14, Color.WHITE, false));
        panel.addView(label((soundsOk ? "✅" : "❌") + " звуковых вариантов: " + GameData.soundVariantCount(), 14, Color.WHITE, false));
        return panel;
    }

    private void prepareRound() {
        stopTimer();
        roundDeck.clear();
        for (int i = 0; i < allCards.size(); i++) {
            if (difficulty == MIXED || (difficulty == EASY && i % 10 < 3) || (difficulty == MEDIUM && i % 10 >= 3 && i % 10 < 7) || (difficulty == HARD && i % 10 >= 7)) {
                roundDeck.add(allCards.get(i));
            }
        }
        Collections.shuffle(roundDeck);
        cardIndex = 0;
        currentTeam = Math.min(currentTeam, teamNames.size() - 1);
        timeLeft = secondsForDifficulty();
        roundPoints = 0;
        roundStarted = false;
    }

    private void showGameScreen() {
        ScrollView scroll = new ScrollView(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(diagonal(new int[]{Color.rgb(15, 23, 42), Color.rgb(76, 29, 149), Color.rgb(190, 24, 93)}, 0));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        LinearLayout content = column();
        content.setPadding(dp(14), dp(16), dp(14), dp(26));
        root.addView(content, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout nav = row();
        TextView home = button3d("⌂ Меню", Color.WHITE, Color.rgb(15, 23, 42), 16);
        home.setOnClickListener(v -> { playSfx("home"); showMainMenu(); });
        TextView reset = button3d("↻ Сброс", Color.argb(55, 255, 255, 255), Color.WHITE, 16);
        reset.setOnClickListener(v -> resetScores());
        TextView win = button3d("🏆 Победитель", Color.rgb(253, 224, 71), Color.rgb(15, 23, 42), 16);
        win.setOnClickListener(v -> showVictory(getWinner()));
        nav.addView(home, new LinearLayout.LayoutParams(0, dp(54), 1));
        nav.addView(reset, new LinearLayout.LayoutParams(0, dp(54), 1));
        nav.addView(win, new LinearLayout.LayoutParams(0, dp(54), 1));
        contentSpacing(nav, dp(8));
        content.addView(nav);

        content.addView(scoreBoard(), paramsTop(-1, -2, 14));

        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(Color.rgb(15, 23, 42), dp(28)));
        panel.addView(label("Сейчас объясняет", 12, Color.argb(170, 255, 255, 255), true));
        teamText = label(teamNames.get(currentTeam), 30, Color.WHITE, true);
        panel.addView(teamText);
        timerText = label(String.valueOf(timeLeft), 72, Color.WHITE, true);
        timerText.setGravity(Gravity.CENTER);
        panel.addView(timerText, paramsTop(-1, -2, 12));

        TextView startPause = button3d(roundStarted ? "▶ Продолжить" : "▶ Старт", Color.rgb(190, 242, 100), Color.rgb(15, 23, 42), 22);
        startPause.setOnClickListener(v -> startTimer());
        panel.addView(startPause, paramsTop(-1, dp(64), 14));
        TextView pause = button3d("⏸ Пауза", Color.argb(55, 255, 255, 255), Color.WHITE, 18);
        pause.setOnClickListener(v -> { stopTimer(); playSfx("pause"); showGameScreen(); });
        panel.addView(pause, paramsTop(-1, dp(56), 10));
        TextView next = button3d("Передать ход", Color.rgb(217, 70, 239), Color.WHITE, 18);
        next.setOnClickListener(v -> nextTeam());
        panel.addView(next, paramsTop(-1, dp(56), 10));
        content.addView(panel, paramsTop(-1, -2, 16));

        LinearLayout card = column();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(round(Color.WHITE, dp(30)));
        topicText = label("карточка · " + currentCard().topic, 12, Color.rgb(100, 116, 139), true);
        card.addView(topicText);
        cardText = label(currentCard().text, 42, Color.WHITE, true);
        cardText.setGravity(Gravity.CENTER);
        cardText.setShadowLayer(7, 0, 5, Color.argb(80, 0, 0, 0));
        FrameLayout wordBox = new FrameLayout(this);
        wordBox.setPadding(dp(12), dp(12), dp(12), dp(12));
        wordBox.setBackground(diagonal(new int[]{Color.rgb(79, 70, 229), Color.rgb(217, 70, 239), Color.rgb(249, 115, 22)}, dp(28)));
        wordBox.addView(cardText, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        card.addView(wordBox, paramsTop(-1, dp(260), 12));

        LinearLayout actions = row();
        TextView correct = button3d("✅ Угадали", Color.rgb(16, 185, 129), Color.WHITE, 20);
        TextView skip = button3d("✖ Пропуск", Color.rgb(244, 63, 94), Color.WHITE, 20);
        correct.setOnClickListener(v -> correctAnswer());
        skip.setOnClickListener(v -> skipCard());
        actions.addView(correct, new LinearLayout.LayoutParams(0, dp(64), 1));
        actions.addView(skip, new LinearLayout.LayoutParams(0, dp(64), 1));
        contentSpacing(actions, dp(10));
        card.addView(actions, paramsTop(-1, -2, 14));

        roundInfoText = label("Раунд: " + roundPoints + " очков. Победа при " + winTarget + ". Доступно карточек: " + roundDeck.size() + ".", 14, Color.rgb(51, 65, 85), false);
        card.addView(roundInfoText, paramsTop(-1, -2, 12));
        TextView reminder = label("Нельзя произносить само слово и однокоренные слова. На карточке только понятие, команда угадывает термин по объяснению.", 14, Color.WHITE, false);
        reminder.setPadding(dp(14), dp(12), dp(14), dp(12));
        reminder.setBackground(round(Color.rgb(15, 23, 42), dp(22)));
        card.addView(reminder, paramsTop(-1, -2, 14));
        content.addView(card, paramsTop(-1, -2, 16));

        setContentView(scroll);
    }

    private View scoreBoard() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = row();
        row.setPadding(0, 0, dp(8), dp(8));
        for (int i = 0; i < teamNames.size(); i++) {
            LinearLayout box = column();
            box.setGravity(Gravity.CENTER);
            box.setPadding(dp(12), dp(10), dp(12), dp(10));
            box.setBackground(round(i == currentTeam ? Color.rgb(253, 224, 71) : Color.WHITE, dp(22)));
            TextView name = label(teamNames.get(i), 13, Color.rgb(15, 23, 42), true);
            name.setGravity(Gravity.CENTER);
            TextView score = label(String.valueOf(scores[i]), 30, Color.rgb(15, 23, 42), true);
            score.setGravity(Gravity.CENTER);
            TextView target = label("цель: " + winTarget, 10, Color.rgb(100, 116, 139), true);
            target.setGravity(Gravity.CENTER);
            box.addView(name);
            box.addView(score);
            box.addView(target);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(132), -2);
            lp.setMargins(0, 0, dp(10), 0);
            row.addView(box, lp);
        }
        hsv.addView(row);
        return hsv;
    }

    private void startTimer() {
        if (timeLeft <= 0) return;
        roundStarted = true;
        playSfx("start");
        stopTimer();
        timer = new CountDownTimer(timeLeft * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                if (timerText != null) {
                    timerText.setText(String.valueOf(timeLeft));
                    timerText.setTextColor(timeLeft <= 10 ? Color.rgb(253, 164, 175) : Color.WHITE);
                }
                if (timeLeft <= 10) playSfx("warning");
            }

            @Override
            public void onFinish() {
                timeLeft = 0;
                if (timerText != null) timerText.setText("0");
                playSfx(roundPoints == 0 ? "defeat" : "modal");
                Toast.makeText(MainActivity.this, "Время вышло", Toast.LENGTH_SHORT).show();
                showGameScreen();
            }
        }.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void correctAnswer() {
        scores[currentTeam]++;
        roundPoints++;
        cardIndex++;
        if (scores[currentTeam] >= winTarget) {
            stopTimer();
            playSfx("victory");
            showVictory(teamNames.get(currentTeam), scores[currentTeam]);
            return;
        }
        playSfx("correct");
        showGameScreen();
    }

    private void skipCard() {
        cardIndex++;
        playSfx("skip");
        showGameScreen();
    }

    private void nextTeam() {
        stopTimer();
        currentTeam = (currentTeam + 1) % teamNames.size();
        timeLeft = secondsForDifficulty();
        roundStarted = false;
        roundPoints = 0;
        cardIndex++;
        playSfx("team");
        showGameScreen();
    }

    private void resetScores() {
        stopTimer();
        for (int i = 0; i < scores.length; i++) scores[i] = 0;
        currentTeam = 0;
        roundStarted = false;
        roundPoints = 0;
        timeLeft = secondsForDifficulty();
        playSfx("reset");
        showGameScreen();
    }

    private Card currentCard() {
        if (roundDeck.isEmpty()) return allCards.get(0);
        return roundDeck.get(Math.abs(cardIndex) % roundDeck.size());
    }

    private String getWinner() {
        int best = 0;
        for (int i = 1; i < teamNames.size(); i++) if (scores[i] > scores[best]) best = i;
        return teamNames.get(best) + "|" + scores[best];
    }

    private void showVictory(String packed) {
        String[] parts = packed.split("\\|", 2);
        int points = parts.length > 1 ? safeInt(parts[1], 0) : 0;
        showVictory(parts[0], points);
    }

    private void showVictory(String name, int points) {
        playSfx(points > 0 ? "victory" : "defeat");
        LinearLayout view = column();
        view.setPadding(dp(20), dp(22), dp(20), dp(18));
        view.setBackground(diagonal(new int[]{Color.rgb(254, 240, 138), Color.rgb(251, 146, 60), Color.rgb(217, 70, 239)}, dp(28)));

        TextView boom = label("БАМ!  ВАУ!", 28, Color.rgb(15, 23, 42), true);
        boom.setGravity(Gravity.CENTER);
        view.addView(boom);
        TextView winner = label("🏆 " + name, 34, Color.rgb(15, 23, 42), true);
        winner.setGravity(Gravity.CENTER);
        view.addView(winner, paramsTop(-1, -2, 8));
        TextView score = label(points + " / " + winTarget + " очков", 22, Color.rgb(51, 65, 85), true);
        score.setGravity(Gravity.CENTER);
        view.addView(score, paramsTop(-1, -2, 4));
        TextView respect = label("РЕСПЕКТ\nОТ САН САНЫЧА!", 46, Color.WHITE, true);
        respect.setGravity(Gravity.CENTER);
        respect.setShadowLayer(9, 7, 8, Color.rgb(15, 23, 42));
        respect.setPadding(0, dp(20), 0, dp(20));
        view.addView(respect);
        TextView ok = button3d("Вернуться к игре", Color.rgb(15, 23, 42), Color.WHITE, 18);
        view.addView(ok, paramsTop(-1, dp(58), 6));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showRules() {
        stopTimer();
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = column();
        content.setPadding(dp(16), dp(18), dp(16), dp(26));
        content.setBackground(round(Color.WHITE, dp(26)));
        scroll.setPadding(dp(14), dp(14), dp(14), dp(14));
        scroll.setBackgroundColor(Color.rgb(15, 23, 42));
        scroll.addView(content);

        TextView back = button3d("Назад в меню", Color.rgb(15, 23, 42), Color.WHITE, 17);
        back.setOnClickListener(v -> showMainMenu());
        content.addView(back, new LinearLayout.LayoutParams(-1, dp(58)));
        content.addView(label("Правила учебного Элиаса", 32, Color.rgb(15, 23, 42), true), paramsTop(-1, -2, 18));
        content.addView(label("Команда получает карточку с понятием по обществознанию. Один участник объясняет термин, не называя само слово и однокоренные формы. Остальные угадывают. За верный ответ начисляется 1 балл. Когда команда набирает выбранное количество очков, появляется победный экран.", 17, Color.rgb(51, 65, 85), false), paramsTop(-1, -2, 12));
        content.addView(label("Словарь объединяет обществознание, политологию, право, экономику, психологию, социальную стратификацию, культуру, демографию, международные отношения, цифровое общество и научные методы. На игровых карточках нет подсказок: только термин для угадывания.", 17, Color.rgb(51, 65, 85), false), paramsTop(-1, -2, 12));
        setContentView(scroll);
    }

    private int secondsForDifficulty() {
        if (difficulty == EASY) return 60;
        if (difficulty == MEDIUM) return 70;
        if (difficulty == HARD) return 80;
        return 75;
    }

    private void resizeScores() {
        int[] next = new int[teamNames.size()];
        for (int i = 0; i < next.length && i < scores.length; i++) next[i] = scores[i];
        scores = next;
    }

    private void updateTeamNamesFromInputs() {
        for (int i = 0; i < teamInputs.size() && i < teamNames.size(); i++) {
            String text = teamInputs.get(i).getText().toString().trim();
            teamNames.set(i, text.length() == 0 ? "Команда " + (i + 1) : text);
        }
    }

    private int safeInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (Exception ignored) { return fallback; }
    }

    private TextView statBox(String icon, String value, String name) {
        LinearLayout box = column();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(12), dp(8), dp(12));
        box.setBackground(round(Color.argb(45, 255, 255, 255), dp(24)));
        TextView all = label(icon + "\n" + value + "\n" + name, 18, Color.WHITE, true);
        all.setGravity(Gravity.CENTER);
        box.addView(all);
        TextView wrapper = new TextView(this);
        wrapper.setVisibility(View.GONE);
        box.setTag(wrapper);
        return wrapAsTextView(box);
    }

    private TextView wrapAsTextView(View view) {
        // Android TextView cannot directly wrap a layout, so this method returns a compact visual fallback for stat cards.
        Object tag = view.getTag();
        TextView fallback = tag instanceof TextView ? (TextView) tag : new TextView(this);
        LinearLayout box = (LinearLayout) view;
        TextView inner = (TextView) box.getChildAt(0);
        fallback.setText(inner.getText());
        fallback.setGravity(Gravity.CENTER);
        fallback.setTextSize(16);
        fallback.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fallback.setTextColor(Color.WHITE);
        fallback.setPadding(dp(8), dp(12), dp(8), dp(12));
        fallback.setBackground(box.getBackground());
        return fallback;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(2), 1.0f);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private TextView button3d(String text, int bgColor, int textColor, int sp) {
        TextView tv = label(text, sp, textColor, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(12), 0, dp(12), 0);
        tv.setBackground(round(bgColor, dp(22)));
        tv.setClickable(true);
        tv.setElevation(dp(8));
        tv.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setTranslationY(dp(4));
                v.setElevation(dp(2));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setTranslationY(0);
                v.setElevation(dp(8));
            }
            return false;
        });
        return tv;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable diagonal(int[] colors, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout.LayoutParams paramsTop(int w, int h, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(0, dp(topDp), 0, 0);
        return lp;
    }

    private void contentSpacing(LinearLayout layout, int spacing) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
            if (i > 0) lp.setMargins(spacing, 0, 0, 0);
            child.setLayoutParams(lp);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void playSfx(String type) {
        if (!soundEnabled) return;
        final String safeType = type == null ? "click" : type;
        new Thread(() -> {
            try {
                int variant = random.nextInt(10);
                int shift = variant * 7;
                if ("correct".equals(safeType)) playSequence(new int[]{640 + shift, 880 + shift}, new int[]{90, 130});
                else if ("wrong".equals(safeType) || "defeat".equals(safeType)) playSequence(new int[]{220 - Math.min(shift, 80), 150 - Math.min(shift / 2, 40)}, new int[]{160, 180});
                else if ("victory".equals(safeType)) playSequence(new int[]{523 + shift, 659 + shift, 784 + shift, 1046 + shift, 1174 + shift}, new int[]{130, 130, 140, 160, 190});
                else if ("warning".equals(safeType)) playSequence(new int[]{980 + shift, 980 + shift}, new int[]{50, 50});
                else if ("pause".equals(safeType)) playSequence(new int[]{420, 260}, new int[]{120, 120});
                else if ("resume".equals(safeType) || "start".equals(safeType)) playSequence(new int[]{360 + shift, 540 + shift, 720 + shift}, new int[]{90, 100, 120});
                else if ("skip".equals(safeType)) playSequence(new int[]{520 + shift, 300 + shift}, new int[]{70, 90});
                else if ("target".equals(safeType)) playSequence(new int[]{500 + shift, 760 + shift}, new int[]{70, 120});
                else playSequence(new int[]{420 + shift, 560 + shift}, new int[]{70, 80});
            } catch (Exception ignored) {}
        }).start();
    }

    private void playSequence(int[] frequencies, int[] durationsMs) throws InterruptedException {
        for (int i = 0; i < frequencies.length; i++) {
            playTone(Math.max(60, frequencies[i]), durationsMs[Math.min(i, durationsMs.length - 1)]);
            Thread.sleep(35);
        }
    }

    private void playTone(int frequency, int durationMs) {
        int sampleRate = 22050;
        int samples = Math.max(1, durationMs * sampleRate / 1000);
        short[] buffer = new short[samples];
        double amp = 0.22 * Short.MAX_VALUE;
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
            double fade = Math.min(1.0, Math.min(i / 120.0, (samples - i) / 160.0));
            buffer[i] = (short) (Math.sin(angle) * amp * Math.max(0.0, fade));
        }
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer.length * 2, AudioTrack.MODE_STATIC);
        track.write(buffer, 0, buffer.length);
        track.play();
        try { Thread.sleep(durationMs + 20L); } catch (InterruptedException ignored) {}
        track.stop();
        track.release();
    }
}
