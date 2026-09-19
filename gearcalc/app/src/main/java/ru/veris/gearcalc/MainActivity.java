package ru.veris.gearcalc;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(20,24,28), CARD=Color.rgb(31,37,43), FIELD=Color.rgb(42,49,56),
            BORDER=Color.rgb(63,72,82), TEXT=Color.rgb(245,247,249), MUTED=Color.rgb(176,186,196),
            ACCENT=Color.rgb(21,177,204), WARN=Color.rgb(255,193,7), BAD=Color.rgb(255,120,120),
            DIALOG_TEXT=Color.rgb(30,33,36);
    private Spinner machineSpinner,toothTypeSpinner,helixSpinner,hobHandSpinner,cutMethodSpinner;
    private EditText zInput,moduleInput,startsInput,betaDegInput,betaMinInput,betaSecInput;
    private LinearLayout helixBox,resultsBox,diffCard;
    private TextView statusText,resultTitle,indexingFormula,indexingRatio,indexingError,indexingMount,
            differentialFormula,differentialRatio,differentialError,differentialMount,setupNote,candidatesText;
    private GearDiagramView indexingDiagram,differentialDiagram;
    private SharedPreferences prefs;
    private MachineConfig machine;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("veris_gear_calc",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout main=new LinearLayout(this); main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(16),dp(14),dp(16),dp(28)); scroll.addView(main,new ScrollView.LayoutParams(-1,-2));

        LinearLayout brandCard=card(), brandRow=row(); brandRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.veris_logo);
        logo.setAdjustViewBounds(true); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        brandRow.addView(logo,new LinearLayout.LayoutParams(dp(72),dp(72)));
        LinearLayout brandText=new LinearLayout(this); brandText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bt=new LinearLayout.LayoutParams(0,-2,1f); bt.setMargins(dp(12),0,0,0);
        TextView brand=text("ВЕРИС",17,ACCENT,true); brand.setLetterSpacing(.08f); brandText.addView(brand);
        brandText.addView(text("Калькулятор гитары",24,TEXT,true));
        TextView bsub=text("Зубофрезерные станки 53А50Н и 5Е32",12,MUTED,false);
        LinearLayout.LayoutParams bs=lp(-1,-2); bs.setMargins(0,dp(4),0,0); brandText.addView(bsub,bs);
        brandRow.addView(brandText,bt); brandCard.addView(brandRow); main.addView(brandCard,lp(-1,-2));

        LinearLayout inputCard=card(); LinearLayout.LayoutParams icp=lp(-1,-2); icp.setMargins(0,dp(12),0,0); main.addView(inputCard,icp);
        machineSpinner=spinner(new String[]{"53А50Н","5Е32"}); inputCard.addView(labeled("Модель станка",machineSpinner));
        toothTypeSpinner=spinner(new String[]{"Прямой зуб","Косой зуб"}); inputCard.addView(labeled("Тип зуба",toothTypeSpinner));

        LinearLayout zr=row(); zInput=numberField("56",false); startsInput=numberField("1",false);
        zr.addView(labeled("Число зубьев z",zInput),halfLeft()); zr.addView(labeled("Заходов фрезы k",startsInput),halfRight()); inputCard.addView(zr);
        moduleInput=numberField("2,5",true); inputCard.addView(labeled("Нормальный модуль mₙ, мм",moduleInput));

        helixBox=new LinearLayout(this); helixBox.setOrientation(LinearLayout.VERTICAL); inputCard.addView(helixBox,lp(-1,-2));
        LinearLayout ar=row(); betaDegInput=numberField("12",false); betaMinInput=numberField("0",false); betaSecInput=numberField("0",true);
        ar.addView(labeled("β, °",betaDegInput),third(0)); ar.addView(labeled("мин",betaMinInput),third(1)); ar.addView(labeled("сек",betaSecInput),third(2)); helixBox.addView(ar);
        helixSpinner=spinner(new String[]{"Правый наклон","Левый наклон"});
        hobHandSpinner=spinner(new String[]{"Правая фреза","Левая фреза"});
        cutMethodSpinner=spinner(new String[]{"Встречное фрезерование","Попутное фрезерование"});
        helixBox.addView(labeled("Направление зуба",helixSpinner));
        helixBox.addView(labeled("Направление витков фрезы",hobHandSpinner));
        helixBox.addView(labeled("Метод фрезерования",cutMethodSpinner));

        statusText=text("",13,BAD,false); statusText.setVisibility(View.GONE);
        LinearLayout.LayoutParams st=lp(-1,-2); st.setMargins(0,dp(10),0,0); inputCard.addView(statusText,st);
        Button calc=primaryButton("Рассчитать"); calc.setOnClickListener(v->calculate());
        LinearLayout.LayoutParams cp=lp(-1,dp(52)); cp.setMargins(0,dp(14),0,0); inputCard.addView(calc,cp);

        LinearLayout buttons=row(); Button stock=secondaryButton("Шестерни в наличии"); stock.setOnClickListener(v->showStockDialog());
        Button clear=secondaryButton("Сбросить"); clear.setOnClickListener(v->resetInputs());
        buttons.addView(stock,halfLeftHeight()); buttons.addView(clear,halfRightHeight());
        LinearLayout.LayoutParams bp=lp(-1,-2); bp.setMargins(0,dp(10),0,0); inputCard.addView(buttons,bp);

        resultsBox=new LinearLayout(this); resultsBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp=lp(-1,-2); rp.setMargins(0,dp(14),0,0); main.addView(resultsBox,rp);
        resultTitle=text("Результат",21,TEXT,true); resultsBox.addView(resultTitle);

        LinearLayout idxCard=card(); LinearLayout.LayoutParams ip=lp(-1,-2); ip.setMargins(0,dp(10),0,0); resultsBox.addView(idxCard,ip);
        idxCard.addView(sectionTitle("Гитара деления")); indexingFormula=valueText(); idxCard.addView(indexingFormula);
        indexingRatio=bigValue(); idxCard.addView(indexingRatio); indexingError=smallValue(); idxCard.addView(indexingError);
        indexingDiagram=new GearDiagramView(this); idxCard.addView(indexingDiagram,lp(-1,dp(210))); indexingMount=bodyText(); idxCard.addView(indexingMount);

        diffCard=card(); LinearLayout.LayoutParams dpv=lp(-1,-2); dpv.setMargins(0,dp(12),0,0); resultsBox.addView(diffCard,dpv);
        diffCard.addView(sectionTitle("Гитара дифференциала")); differentialFormula=valueText(); diffCard.addView(differentialFormula);
        differentialRatio=bigValue(); diffCard.addView(differentialRatio); differentialError=smallValue(); diffCard.addView(differentialError);
        differentialDiagram=new GearDiagramView(this); diffCard.addView(differentialDiagram,lp(-1,dp(210))); differentialMount=bodyText(); diffCard.addView(differentialMount);

        LinearLayout note=card(); LinearLayout.LayoutParams np=lp(-1,-2); np.setMargins(0,dp(12),0,0); resultsBox.addView(note,np);
        note.addView(sectionTitle("Установка и проверка")); setupNote=bodyText(); note.addView(setupNote);
        candidatesText=bodyText(); LinearLayout.LayoutParams cap=lp(-1,-2); cap.setMargins(0,dp(10),0,0); note.addView(candidatesText,cap);
        TextView footer=text("ВЕРИС • офлайн-расчёт • схема установки не в масштабе",12,MUTED,false);
        LinearLayout.LayoutParams fp=lp(-1,-2); fp.setMargins(0,dp(14),0,0); main.addView(footer,fp);

        machineSpinner.setSelection(prefs.getInt("last_machine",0)); toothTypeSpinner.setSelection(prefs.getInt("last_tooth_type",0));
        machine=machineSpinner.getSelectedItemPosition()==0?MachineConfig.m53():MachineConfig.m5e32();
        toothTypeSpinner.setOnItemSelectedListener(new SimpleSelection(){ public void selected(int p){helixBox.setVisibility(p==1?View.VISIBLE:View.GONE);calculate();}});
        machineSpinner.setOnItemSelectedListener(new SimpleSelection(){ public void selected(int p){machine=p==0?MachineConfig.m53():MachineConfig.m5e32();calculate();}});
        helixBox.setVisibility(toothTypeSpinner.getSelectedItemPosition()==1?View.VISIBLE:View.GONE);
        setContentView(scroll); calculate();
    }

    private void calculate(){
        if(machineSpinner==null)return;
        machine=machineSpinner.getSelectedItemPosition()==0?MachineConfig.m53():MachineConfig.m5e32();
        int z=(int)parse(zInput.getText().toString()), k=(int)parse(startsInput.getText().toString());
        double mn=parse(moduleInput.getText().toString()), beta=parseAngle(); boolean helical=toothTypeSpinner.getSelectedItemPosition()==1;
        if(z<=0||k<=0||Double.isNaN(mn)||mn<=0||(helical&&(Double.isNaN(beta)||beta<=0||beta>=90))){
            statusText.setText("Проверьте входные данные: z, k и модуль должны быть больше нуля; для косого зуба β — от 0° до 90°.");
            statusText.setVisibility(View.VISIBLE); resultsBox.setVisibility(View.GONE); return;
        }
        statusText.setVisibility(View.GONE); resultsBox.setVisibility(View.VISIBLE);
        prefs.edit().putInt("last_machine",machineSpinner.getSelectedItemPosition()).putInt("last_tooth_type",toothTypeSpinner.getSelectedItemPosition()).apply();

        double target=(z<=161?24.0:48.0)*k/z; List<Candidate> idx=GearSolver.solve(target,available(machine),5); Candidate c=idx.isEmpty()?null:idx.get(0);
        indexingFormula.setText("Требуемое отношение: "+(z<=161?"24·k/z":"48·k/z")+" = "+fmt(target,8)+"   •   перебор e/f: "+(z<=161?machine.lowEf:machine.highEf));
        if(c==null){indexingRatio.setText("Нет комбинации");indexingError.setText("Проверьте набор шестерён в наличии.");indexingMount.setText("");indexingDiagram.setCandidate(null,false);}
        else fillResult(c,indexingRatio,indexingError,indexingMount,indexingDiagram,false);

        List<Candidate> diffs=Collections.emptyList();
        if(helical){
            diffCard.setVisibility(View.VISIBLE); double dt=7.95775*Math.sin(Math.toRadians(beta))/(mn*k);
            diffs=GearSolver.solve(dt,available(machine),5); Candidate dc=diffs.isEmpty()?null:diffs.get(0);
            differentialFormula.setText("Требуемое отношение: 7,95775·sinβ/(mₙ·k) = "+fmt(dt,8));
            if(dc==null){differentialRatio.setText("Нет комбинации");differentialError.setText("Проверьте набор шестерён в наличии.");differentialMount.setText("");differentialDiagram.setCandidate(null,true);}
            else fillResult(dc,differentialRatio,differentialError,differentialMount,differentialDiagram,true);
        } else diffCard.setVisibility(View.GONE);

        resultTitle.setText(machine.name+" • z="+z+" • mₙ="+fmt(mn,3)+(helical?" • β="+angleText(beta):" • прямой зуб"));
        setupNote.setText(buildSetupNote(helical,beta)); candidatesText.setText(buildAlternatives(idx,diffs,helical));
    }

    private void fillResult(Candidate c,TextView ratio,TextView err,TextView mount,GearDiagramView diagram,boolean diff){
        ratio.setText(c.expression()); double ppm=c.relError*1000000.0;
        String q=c.relError<=1e-6?"очень точно":(c.relError<=1e-4?"точно":"проверьте допуск");
        err.setText("Фактическое отношение "+fmt(c.actual,9)+" • ошибка "+fmt(c.relError*100,6)+"% ("+fmt(ppm,1)+" ppm) • "+q);
        String a=diff?"a₁":"A",b=diff?"b₁":"B",cc=diff?"c₁":"C",d=diff?"d₁":"D";
        if(c.twoGear) mount.setText(a+" = "+c.a+" — ведущая; "+b+" = "+c.b+" — ведомая. Вторую пару не ставить. Промежуточное колесо, если нужно только для направления вращения, передаточное отношение не меняет.");
        else mount.setText(a+" = "+c.a+" — ведущая; сцепить с "+b+" = "+c.b+". На одном валу с "+b+" поставить "+cc+" = "+c.c+"; "+cc+" сцепить с "+d+" = "+c.d+" — ведомой.");
        diagram.setCandidate(c,diff);
    }

    private String buildSetupNote(boolean h,double beta){
        if(!h)return "Для прямозубого колеса используется гитара деления. После установки прокрутите станок вручную и убедитесь в свободном зацеплении сменных колёс. Схема показывает порядок A→B, B и C на одной оси, C→D.";
        String gh=helixSpinner.getSelectedItemPosition()==0?"правый":"левый", hob=hobHandSpinner.getSelectedItemPosition()==0?"правая":"левая", m=cutMethodSpinner.getSelectedItemPosition()==0?"встречное":"попутное";
        return "Косой зуб: "+gh+" наклон, "+hob.toLowerCase()+" фреза, "+m+" фрезерование, β="+angleText(beta)+". Перед запуском вручную проверьте направление вращения стола, работу дифференциала и отсутствие закусывания сменных колёс.";
    }

    private String buildAlternatives(List<Candidate> idx,List<Candidate> diff,boolean h){
        StringBuilder s=new StringBuilder("Ближайшие варианты\\nДеление: "); appendCandidates(s,idx);
        if(h){s.append("\\nДифференциал: ");appendCandidates(s,diff);} return s.toString();
    }
    private void appendCandidates(StringBuilder s,List<Candidate> list){int n=Math.min(3,list.size());for(int i=0;i<n;i++){if(i>0)s.append("  •  ");Candidate c=list.get(i);s.append(c.expression()).append(" (Δ ").append(fmt(c.relError*100,5)).append("%)");}if(n==0)s.append("нет");}

    private void showStockDialog(){
        machine=machineSpinner.getSelectedItemPosition()==0?MachineConfig.m53():MachineConfig.m5e32();
        final LinkedHashMap<Integer,Integer> stock=loadStock(machine);
        final LinkedHashMap<Integer,Integer> custom=loadCustomStock(machine);
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(Color.WHITE);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18),dp(8),dp(18),dp(12)); box.setBackgroundColor(Color.WHITE); scroll.addView(box);

        TextView stdTitle=text("Паспортный комплект",16,DIALOG_TEXT,true);
        LinearLayout.LayoutParams titleLp=lp(-1,-2); titleLp.setMargins(0,dp(4),0,dp(6)); box.addView(stdTitle,titleLp);

        for(Map.Entry<Integer,Integer> e:machine.maxCounts().entrySet()){
            int teeth=e.getKey(), maximum=e.getValue();
            LinearLayout r=row(); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0,dp(2),0,dp(2));
            TextView lab=text(teeth+" зубьев",17,DIALOG_TEXT,true); r.addView(lab,new LinearLayout.LayoutParams(0,dp(48),1f));
            Button count=new Button(this); count.setAllCaps(false); count.setTextSize(16); count.setTypeface(Typeface.DEFAULT_BOLD); count.setTextColor(Color.WHITE);
            count.setBackground(roundRect(Color.rgb(45,52,60),Color.rgb(80,88,96),12));
            Runnable refresh=()->count.setText(stock.get(teeth)+" / "+maximum); refresh.run();
            count.setOnClickListener(v->{int now=stock.get(teeth)-1;if(now<0)now=maximum;stock.put(teeth,now);refresh.run();});
            r.addView(count,new LinearLayout.LayoutParams(dp(110),dp(44))); box.addView(r);
            View div=new View(this); div.setBackgroundColor(Color.rgb(232,235,238)); box.addView(div,new LinearLayout.LayoutParams(-1,dp(1)));
        }

        TextView customTitle=text("Дополнительные шестерни",16,DIALOG_TEXT,true);
        LinearLayout.LayoutParams ctLp=lp(-1,-2); ctLp.setMargins(0,dp(16),0,dp(6)); box.addView(customTitle,ctLp);

        if(custom.isEmpty()){
            TextView none=text("Пока нет. Можно добавить любую реально имеющуюся шестерню, например 91 зуб.",14,Color.rgb(95,100,105),false);
            LinearLayout.LayoutParams noneLp=lp(-1,-2); noneLp.setMargins(0,0,0,dp(8)); box.addView(none,noneLp);
        } else {
            for(Map.Entry<Integer,Integer> e:custom.entrySet()){
                int teeth=e.getKey();
                LinearLayout r=row(); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0,dp(2),0,dp(2));
                TextView lab=text(teeth+" зубьев  •  доп.",17,DIALOG_TEXT,true); r.addView(lab,new LinearLayout.LayoutParams(0,dp(48),1f));
                Button qty=new Button(this); qty.setAllCaps(false); qty.setTextSize(15); qty.setTypeface(Typeface.DEFAULT_BOLD); qty.setTextColor(Color.WHITE);
                qty.setText(e.getValue()+" шт."); qty.setBackground(roundRect(Color.rgb(45,52,60),Color.rgb(80,88,96),12));
                qty.setOnClickListener(v->showCustomCountDialog(machine,teeth));
                r.addView(qty,new LinearLayout.LayoutParams(dp(92),dp(44)));
                Button del=new Button(this); del.setAllCaps(false); del.setText("×"); del.setTextSize(20); del.setTextColor(Color.rgb(150,30,30));
                del.setBackgroundColor(Color.TRANSPARENT); del.setOnClickListener(v->{removeCustomGear(machine,teeth);showStockDialog();});
                LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(dp(46),dp(44)); dl.setMargins(dp(4),0,0,0); r.addView(del,dl);
                box.addView(r);
                View div=new View(this); div.setBackgroundColor(Color.rgb(232,235,238)); box.addView(div,new LinearLayout.LayoutParams(-1,dp(1)));
            }
        }

        Button add=new Button(this); add.setAllCaps(false); add.setText("+ Добавить шестерню"); add.setTextSize(16); add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setTextColor(Color.WHITE); add.setBackground(roundRect(Color.rgb(21,140,165),Color.rgb(21,177,204),12));
        LinearLayout.LayoutParams addLp=lp(-1,dp(48)); addLp.setMargins(0,dp(10),0,dp(4)); box.addView(add,addLp);
        add.setOnClickListener(v->showAddCustomGearDialog(machine));

        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(machine.name+" — шестерни в наличии")
                .setMessage("Паспортные колёса можно отключать. Дополнительные колёса участвуют в подборе наравне со штатными.")
                .setView(scroll).setPositiveButton("Сохранить",(d,w)->{saveStock(machine,stock);calculate();})
                .setNeutralButton("Паспортный комплект",(d,w)->{resetStock(machine);calculate();})
                .setNegativeButton("Закрыть",null).create();
        dialog.setOnShowListener(x->{TextView msg=dialog.findViewById(android.R.id.message);if(msg!=null){msg.setTextColor(DIALOG_TEXT);msg.setTextSize(16);}});
        dialog.show();
    }

    private void showAddCustomGearDialog(MachineConfig m){
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setPadding(dp(20),dp(6),dp(20),0);
        EditText teeth=new EditText(this); teeth.setHint("Например, 91"); teeth.setInputType(InputType.TYPE_CLASS_NUMBER); teeth.setTextColor(DIALOG_TEXT); teeth.setHintTextColor(Color.GRAY);
        EditText qty=new EditText(this); qty.setHint("Количество, по умолчанию 1"); qty.setInputType(InputType.TYPE_CLASS_NUMBER); qty.setTextColor(DIALOG_TEXT); qty.setHintTextColor(Color.GRAY);
        wrap.addView(teeth,lp(-1,dp(54))); wrap.addView(qty,lp(-1,dp(54)));
        new AlertDialog.Builder(this).setTitle("Добавить шестерню").setView(wrap)
                .setPositiveButton("Добавить",(d,w)->{
                    int t=(int)parse(teeth.getText().toString()); int q=(int)parse(qty.getText().toString());
                    if(q<=0)q=1;
                    if(t>=10&&t<=300){addCustomGear(m,t,q);calculate();}
                }).setNegativeButton("Отмена",null).show();
    }

    private void showCustomCountDialog(MachineConfig m,int teeth){
        EditText qty=new EditText(this); qty.setInputType(InputType.TYPE_CLASS_NUMBER); qty.setSelectAllOnFocus(true);
        qty.setText(String.valueOf(loadCustomStock(m).get(teeth))); qty.setTextColor(DIALOG_TEXT);
        LinearLayout wrap=new LinearLayout(this); wrap.setPadding(dp(22),0,dp(22),0); wrap.addView(qty,lp(-1,dp(56)));
        new AlertDialog.Builder(this).setTitle(teeth+" зубьев — количество").setView(wrap)
                .setPositiveButton("Сохранить",(d,w)->{int q=(int)parse(qty.getText().toString());if(q>0)addCustomGear(m,teeth,q);else removeCustomGear(m,teeth);calculate();})
                .setNegativeButton("Отмена",null).show();
    }

    private LinkedHashMap<Integer,Integer> loadStock(MachineConfig m){LinkedHashMap<Integer,Integer> out=new LinkedHashMap<>();for(Map.Entry<Integer,Integer> e:m.maxCounts().entrySet())out.put(e.getKey(),prefs.getInt("stock_"+m.key+"_"+e.getKey(),e.getValue()));return out;}
    private void saveStock(MachineConfig m,Map<Integer,Integer> stock){SharedPreferences.Editor ed=prefs.edit();for(Map.Entry<Integer,Integer> e:stock.entrySet())ed.putInt("stock_"+m.key+"_"+e.getKey(),e.getValue());ed.apply();}
    private void resetStock(MachineConfig m){SharedPreferences.Editor ed=prefs.edit();for(Integer t:m.maxCounts().keySet())ed.remove("stock_"+m.key+"_"+t);ed.apply();}

    private LinkedHashMap<Integer,Integer> loadCustomStock(MachineConfig m){
        LinkedHashMap<Integer,Integer> out=new LinkedHashMap<>();
        String csv=prefs.getString("custom_list_"+m.key,"");
        if(csv==null||csv.trim().isEmpty())return out;
        String[] parts=csv.split(",");
        List<Integer> teeth=new ArrayList<>();
        for(String s:parts){try{int t=Integer.parseInt(s.trim());if(t>0&&!teeth.contains(t))teeth.add(t);}catch(Exception ignored){}}
        Collections.sort(teeth);
        for(Integer t:teeth){int q=prefs.getInt("custom_count_"+m.key+"_"+t,1);if(q>0)out.put(t,q);}
        return out;
    }
    private void addCustomGear(MachineConfig m,int teeth,int count){
        LinkedHashMap<Integer,Integer> map=loadCustomStock(m); map.put(teeth,Math.max(1,count)); saveCustomStock(m,map);
    }
    private void removeCustomGear(MachineConfig m,int teeth){
        LinkedHashMap<Integer,Integer> map=loadCustomStock(m); map.remove(teeth); saveCustomStock(m,map);
    }
    private void saveCustomStock(MachineConfig m,Map<Integer,Integer> map){
        StringBuilder csv=new StringBuilder(); SharedPreferences.Editor ed=prefs.edit();
        List<Integer> keys=new ArrayList<>(map.keySet()); Collections.sort(keys);
        for(Integer t:keys){if(csv.length()>0)csv.append(",");csv.append(t);ed.putInt("custom_count_"+m.key+"_"+t,map.get(t));}
        ed.putString("custom_list_"+m.key,csv.toString()).apply();
    }

    private List<Integer> available(MachineConfig m){
        List<Integer> out=new ArrayList<>();
        for(Map.Entry<Integer,Integer> e:loadStock(m).entrySet())for(int i=0;i<e.getValue();i++)out.add(e.getKey());
        for(Map.Entry<Integer,Integer> e:loadCustomStock(m).entrySet())for(int i=0;i<e.getValue();i++)out.add(e.getKey());
        return out;
    }
    private void resetInputs(){zInput.setText("56");moduleInput.setText("2,5");startsInput.setText("1");betaDegInput.setText("12");betaMinInput.setText("0");betaSecInput.setText("0");toothTypeSpinner.setSelection(0);helixSpinner.setSelection(0);hobHandSpinner.setSelection(0);cutMethodSpinner.setSelection(0);calculate();}
    private double parseAngle(){double d=parse(betaDegInput.getText().toString()),m=parse(betaMinInput.getText().toString()),s=parse(betaSecInput.getText().toString());if(Double.isNaN(d)||Double.isNaN(m)||Double.isNaN(s)||m<0||m>=60||s<0||s>=60)return Double.NaN;return d+m/60.0+s/3600.0;}
    private String angleText(double beta){int d=(int)Math.floor(beta);double rem=(beta-d)*60;int m=(int)Math.floor(rem),s=(int)Math.round((rem-m)*60);if(s==60){s=0;m++;}if(m==60){m=0;d++;}return d+"° "+m+"′ "+s+"″";}
    private double parse(String s){try{return Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private String fmt(double v,int max){DecimalFormatSymbols sym=new DecimalFormatSymbols(new Locale("ru","RU"));sym.setDecimalSeparator(',');return new DecimalFormat("0."+repeat('#',max),sym).format(v);}
    private String repeat(char c,int n){StringBuilder s=new StringBuilder();for(int i=0;i<n;i++)s.append(c);return s.toString();}

    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(14),dp(14),dp(14),dp(14));l.setBackground(roundRect(CARD,BORDER,16));return l;}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);return r;}
    private LinearLayout labeled(String label,View child){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.addView(text(label,12,MUTED,false));LinearLayout.LayoutParams cp=lp(-1,child instanceof Spinner?dp(50):dp(48));cp.setMargins(0,dp(3),0,0);box.addView(child,cp);LinearLayout.LayoutParams p=lp(-1,-2);p.setMargins(0,0,0,dp(10));box.setLayoutParams(p);return box;}
    private EditText numberField(String initial,boolean decimal){EditText e=new EditText(this);e.setText(initial);e.setTextColor(TEXT);e.setTextSize(19);e.setTypeface(Typeface.DEFAULT_BOLD);e.setSingleLine(true);e.setSelectAllOnFocus(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(roundRect(FIELD,BORDER,10));int type=InputType.TYPE_CLASS_NUMBER;if(decimal)type|=InputType.TYPE_NUMBER_FLAG_DECIMAL;e.setInputType(type);e.setImeOptions(EditorInfo.IME_ACTION_NEXT);return e;}
    private Spinner spinner(String[] values){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values){@Override public View getView(int p,View v,ViewGroup g){TextView t=(TextView)super.getView(p,v,g);t.setTextColor(TEXT);t.setTextSize(16);t.setPadding(dp(10),0,dp(10),0);return t;}@Override public View getDropDownView(int p,View v,ViewGroup g){TextView t=(TextView)super.getDropDownView(p,v,g);t.setTextColor(Color.BLACK);t.setTextSize(16);t.setPadding(dp(14),dp(12),dp(14),dp(12));return t;}};s.setAdapter(a);s.setBackground(roundRect(FIELD,BORDER,10));return s;}
    private Button primaryButton(String label){Button b=button(label);b.setTextColor(Color.rgb(8,24,30));b.setBackground(roundRect(ACCENT,ACCENT,12));return b;}
    private Button secondaryButton(String label){Button b=button(label);b.setTextColor(TEXT);b.setBackground(roundRect(FIELD,BORDER,12));return b;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT_BOLD);return b;}
    private TextView sectionTitle(String s){TextView t=text(s,17,TEXT,true);LinearLayout.LayoutParams p=lp(-1,-2);p.setMargins(0,0,0,dp(6));t.setLayoutParams(p);return t;}
    private TextView valueText(){return text("",13,MUTED,false);} private TextView bigValue(){TextView t=text("—",28,ACCENT,true);LinearLayout.LayoutParams p=lp(-1,-2);p.setMargins(0,dp(6),0,0);t.setLayoutParams(p);return t;}
    private TextView smallValue(){return text("",12,MUTED,false);} private TextView bodyText(){TextView t=text("",14,TEXT,false);t.setLineSpacing(0,1.12f);return t;}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);return t;}
    private GradientDrawable roundRect(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);} private LinearLayout.LayoutParams halfLeft(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);p.setMargins(0,0,dp(5),0);return p;}
    private LinearLayout.LayoutParams halfRight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);p.setMargins(dp(5),0,0,0);return p;} private LinearLayout.LayoutParams halfLeftHeight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1f);p.setMargins(0,0,dp(5),0);return p;}
    private LinearLayout.LayoutParams halfRightHeight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1f);p.setMargins(dp(5),0,0,0);return p;} private LinearLayout.LayoutParams third(int i){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);p.setMargins(i==0?0:dp(4),0,i==2?0:dp(4),0);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private abstract class SimpleSelection implements AdapterView.OnItemSelectedListener{public abstract void selected(int p);@Override public void onItemSelected(AdapterView<?> parent,View view,int p,long id){selected(p);}@Override public void onNothingSelected(AdapterView<?> parent){}}

    static class MachineConfig{
        final String key,name,lowEf,highEf;final int[]gears;MachineConfig(String k,String n,String l,String h,int[]g){key=k;name=n;lowEf=l;highEf=h;gears=g;}
        static MachineConfig m53(){return new MachineConfig("53a50n","53А50Н","54/54 = 1:1","36/72 = 1:2",new int[]{24,25,25,27,28,30,33,34,35,37,40,40,41,43,45,47,48,50,53,55,58,59,60,61,62,65,66,67,70,70,71,73,75,79,80,83,85,87,89,90,92,95,97,98,100});}
        static MachineConfig m5e32(){return new MachineConfig("5e32","5Е32","36/36 = 1:1","24/48 = 1:2",new int[]{24,25,25,30,33,34,35,37,40,41,43,45,47,48,50,53,55,57,58,59,60,61,62,65,67,70,71,73,75,79,80,83,85,89,90,92,95,97,98,100});}
        LinkedHashMap<Integer,Integer>maxCounts(){LinkedHashMap<Integer,Integer>m=new LinkedHashMap<>();for(int g:gears)m.put(g,m.containsKey(g)?m.get(g)+1:1);return m;}
    }
    static class Candidate{int a,b,c,d;boolean twoGear;double actual,relError;Candidate(int a,int b,int c,int d,boolean t,double target){this.a=a;this.b=b;this.c=c;this.d=d;twoGear=t;actual=t?(double)a/b:(double)a/b*(double)c/d;relError=Math.abs(actual-target)/Math.abs(target);}String expression(){return twoGear?a+" / "+b:a+" / "+b+" × "+c+" / "+d;}}
    static class GearSolver{
        static List<Candidate>solve(double target,List<Integer>gears,int limit){List<Candidate>best=new ArrayList<>();if(target<=0||gears.size()<2)return best;for(int i=0;i<gears.size();i++)for(int j=0;j<gears.size();j++)if(i!=j)offer(best,new Candidate(gears.get(i),gears.get(j),0,0,true,target),limit);
            for(int i=0;i<gears.size();i++)for(int j=0;j<gears.size();j++){if(i==j)continue;double first=(double)gears.get(i)/gears.get(j);for(int k=0;k<gears.size();k++){if(k==i||k==j)continue;double need=target/first;int bestL=-1;double bestErr=Double.MAX_VALUE;for(int l=0;l<gears.size();l++){if(l==i||l==j||l==k)continue;double r=(double)gears.get(k)/gears.get(l),e=Math.abs(r-need);if(e<bestErr){bestErr=e;bestL=l;}}if(bestL>=0)offer(best,new Candidate(gears.get(i),gears.get(j),gears.get(k),gears.get(bestL),false,target),limit);}}
            Collections.sort(best,cmp());return dedupe(best,limit);}
        static void offer(List<Candidate>l,Candidate c,int n){l.add(c);if(l.size()>n*12){Collections.sort(l,cmp());while(l.size()>n*6)l.remove(l.size()-1);}}
        static Comparator<Candidate>cmp(){return(x,y)->{int e=Double.compare(x.relError,y.relError);if(e!=0)return e;if(x.twoGear!=y.twoGear)return x.twoGear?-1:1;return Integer.compare(x.a+x.b+x.c+x.d,y.a+y.b+y.c+y.d);};}
        static List<Candidate>dedupe(List<Candidate>in,int n){List<Candidate>out=new ArrayList<>();for(Candidate c:in){boolean same=false;for(Candidate o:out)if(c.expression().equals(o.expression())){same=true;break;}if(!same)out.add(c);if(out.size()>=n)break;}return out;}
    }

    class GearDiagramView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Candidate c;boolean differential;GearDiagramView(Activity a){super(a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setCandidate(Candidate c,boolean d){this.c=c;differential=d;invalidate();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);int w=getWidth(),h=getHeight();if(c==null){p.setColor(MUTED);p.setTextSize(dp(14));canvas.drawText("Нет схемы",dp(10),h/2f,p);return;}String[]labels=differential?new String[]{"a₁","b₁","c₁","d₁"}:new String[]{"A","B","C","D"};float cy=h*.53f;
            if(c.twoGear){drawGear(canvas,w*.32f,cy,c.a,labels[0]);drawGear(canvas,w*.68f,cy,c.b,labels[1]);drawMesh(canvas,w*.32f,w*.68f,cy);drawCaption(canvas,"ведущая",w*.32f,h-dp(8));drawCaption(canvas,"ведомая",w*.68f,h-dp(8));}
            else{float x1=w*.18f,x2=w*.40f,x3=w*.60f,x4=w*.82f;drawGear(canvas,x1,cy,c.a,labels[0]);drawGear(canvas,x2,cy,c.b,labels[1]);drawGear(canvas,x3,cy,c.c,labels[2]);drawGear(canvas,x4,cy,c.d,labels[3]);drawMesh(canvas,x1,x2,cy);drawShaft(canvas,x2,x3,cy);drawMesh(canvas,x3,x4,cy);drawCaption(canvas,"ведущая",x1,h-dp(8));drawCaption(canvas,"один вал",(x2+x3)/2,h-dp(8));drawCaption(canvas,"ведомая",x4,h-dp(8));}
            p.setTypeface(Typeface.DEFAULT);p.setTextSize(dp(11));p.setColor(MUTED);canvas.drawText("Схема, не в масштабе",dp(4),dp(14),p);}
        void drawGear(Canvas c,float x,float y,int teeth,String label){float r=dp(30)+Math.min(dp(11),teeth/9f);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(48,66,75));c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(ACCENT);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);p.setColor(BG);c.drawCircle(x,y,dp(8),p);p.setTextAlign(Paint.Align.CENTER);p.setColor(TEXT);p.setTextSize(dp(13));p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(label+" "+teeth,x,y+dp(5),p);p.setTextAlign(Paint.Align.LEFT);}
        void drawMesh(Canvas c,float x1,float x2,float y){p.setColor(WARN);p.setStrokeWidth(dp(2));p.setStyle(Paint.Style.STROKE);Path path=new Path();path.moveTo(x1+dp(34),y-dp(4));path.lineTo(x2-dp(34),y+dp(4));c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
        void drawShaft(Canvas c,float x1,float x2,float y){p.setColor(MUTED);p.setStrokeWidth(dp(5));c.drawLine(x1,y-dp(48),x2,y-dp(48),p);c.drawLine(x1,y-dp(48),x1,y-dp(35),p);c.drawLine(x2,y-dp(48),x2,y-dp(35),p);}
        void drawCaption(Canvas c,String s,float x,float y){p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(10));p.setTypeface(Typeface.DEFAULT);p.setColor(MUTED);c.drawText(s,x,y,p);p.setTextAlign(Paint.Align.LEFT);}
    }
}
