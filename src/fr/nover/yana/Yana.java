/**
*Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
*Elle a �t� cr��e afin d'interagir avec YANA, lui-m�me cr�� par Idleman.
*Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
*Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yana;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import fr.nover.yana.assistant_installation.Assistant_Installation;
import fr.nover.yana.passerelles.ExpandableListAdapter;
import fr.nover.yana.passerelles.Traitement;
import fr.nover.yana.passerelles.ShakeDetector;

public class Yana extends Activity implements TextToSpeech.OnInitListener{
	
	static EditText IPadress; // Affiche et stocke l'adresse IP
	static TextView tts_pref_false; // Affichage pour pr�venir de l'�tat du TTS
	ImageButton btnRec; // Bouton pour lancer l'initialisation
	ImageView ip_adress; // Affichage et actions du bouton � c�t� de ip_adress
	String Recrep="", Rep=""; // D�clare les variables correspondant aux divers �l�ments de la conversation avec le RPi
    String Nom, Pr�nom, Sexe, Pseudo; // Pour l'identit� de l'utilisateur
    boolean bienvenue;
    static boolean bienvenue_fait=false;
    Random random = new Random(); // Pour un message al�atoire
		
    private TextToSpeech mTts;// D�clare le TTS
    
    static boolean testTTS = false, testMAJ = false, AI, TTS_TEST, Commande_actu=false;
	    
    	// A propos du Service (Intent pour le lancer et servstate pour savoir l'�tat du service)
	private Intent ShakeService;
	public static boolean servstate=false;
	boolean Box_TTS, Box_TTS_presence;
	
	String Token="";
	
	SharedPreferences.Editor geted;
	
		// Conversation et liste de commandes
	int n=1;
	boolean update;
	Handler myHandler = new Handler();
	
	ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    ArrayList<String> listDataHeader;
    HashMap<String, ArrayList<String>> listDataChild;
	
		// S'il re�oit un signal Broadcast du Service, il r�agit en cons�quence
	private BroadcastReceiver NewRecrep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "envoi");}};
			
	private BroadcastReceiver NewRep = new BroadcastReceiver() { 
		  @Override
		  public void onReceive(Context context, Intent intent) {
			String contenu = intent.getStringExtra("contenu");
			conversation(contenu, "reponse");}};
	    
	    // Juste une valeur fixe de r�f�rence pour le r�sultat d'Activit�s lanc�es
	protected static final int RESULT_SPEECH = 1;
	protected static final int OPTION = 2;
	protected static final int TTS = 3;
	
	public void onCreate(Bundle savedInstanceState){
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.interface_yana); // D�finit la layout a utiliser
	    
	    LocalBroadcastManager.getInstance(this).registerReceiver(NewRecrep, // D�clare les liens Broadcast avec le Service
	 			new IntentFilter("NewRecrep"));
	    LocalBroadcastManager.getInstance(this).registerReceiver(NewRep,
				new IntentFilter("NewRep"));

    	IPadress = (EditText)findViewById(R.id.IPadress); // D�clare les �l�ments visibles
    	tts_pref_false = (TextView) findViewById(R.id.tts_pref_false);
    	btnRec = (ImageButton) findViewById(R.id.btnRec);
    	ip_adress = (ImageView) findViewById(R.id.ip_adress);
    	
    	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); // Emp�che un bug de contact avec le RPi (je ne sais pas pourquoi :))
    	StrictMode.setThreadPolicy(policy);

		geted = PreferenceManager.getDefaultSharedPreferences(this).edit(); // Pour �diter les options
    	
		IPadress.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT); // D�finit l'EditText comme un champ URL
    	
    	getConfig(); // Actualise la configuration
    	if(!Commande_actu){Commandes_actu();}
    	if(update){Commandes_actu();} // Actualise les commandes si la config correspond
    		
    	ip_adress.setOnClickListener(new View.OnClickListener() { // Lance la configuration si on clique sur l'image � c�t� de l'adresse IP
    		@Override
    		public void onClick(View v){
    			String IP_Adress=IPadress.getText().toString();
    			if(IP_Adress.contains("action.php")){
    				IP_Adress = IP_Adress.replace("action.php", "");
        			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+IP_Adress));
        			startActivity(browserIntent);}
    			else{
    				Toast toast= Toast.makeText(getApplicationContext(),
    			    "Votre adresse n'est pas bonne. :(", 4000);  
    				toast.show();}
    			}});
    	
    	btnRec.setOnClickListener(new View.OnClickListener() {	 // S'effectue lors d'un appui sur le bouton Rec
    		@Override
    		public void onClick(View v){
    			Initialisation();}});}
	
	public void onStart(){
	    super.onStart();
	    
    	if(AI){    		
    		Intent SetupWizard = new Intent(this, Assistant_Installation.class);
    		startActivityForResult(SetupWizard, OPTION);}
	    
    	else{
		   	if(bienvenue && Box_TTS && !bienvenue_fait){
		   		bienvenue_fait=true;
		   		Rep = Random_String();
		   		mTts = new TextToSpeech(this, this);}
	}}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data){ // S'ex�cute lors d'un retour d'activit�
    switch (requestCode) {
		case RESULT_SPEECH: { // D�s que la reconnaissance vocale est termin�e
			if (resultCode == RESULT_OK && null != data) {
				
				ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				Recrep = text.get(0); // Enregistre le r�sultat dans RecRep
				
				String Ordre="", URL=""; // D�clare Ordre
				int n = Traitement.Comparaison(Recrep); // Compare les String pour trouver un ordre
				if(n<0){Ordre=Recrep;} // Si la comparaison a �chou�
				else{ // Sinon, la commande la plus proche de l'ordre est attribu�e � Ordre
					Ordre = Traitement.Commandes.get(n); 
					URL = Traitement.Liens.get(n);}
	
				Pr�traitement(Ordre, URL); // Envoie en Pr�traitement
				break;}}
		
		case OPTION: // D�s un retour de la configuration, il la recharge
			getConfig();
			break;
	}}

	public void onInit(int i){ // S'ex�cute d�s la cr�ation du mTts
			if(mTts.isLanguageAvailable(Locale.FRENCH)!=TextToSpeech.LANG_AVAILABLE && !testTTS){
				new AlertDialog.Builder(this)
		 	    .setTitle("Le TTS n'est pas en Fran�ais.")
		 	    .setMessage("Android d�tecte que votre dispositif de Synth�se Vocale ne dispose pas du Fran�ais dans ses langues. Voulez-vous installer le Fran�ais ? Appuyez sur Non pour continuer quand m�me.")
		 	    .setNegativeButton("Non", new DialogInterface.OnClickListener() {
		 	        public void onClick(DialogInterface dialog, int which) { 
		 	            testTTS=true;}
		 	     })
		 	    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
		 	        public void onClick(DialogInterface dialog, int which) { 
		 	        	Intent installIntent = new Intent();
			            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			            startActivity(installIntent);}
		 	     })
		 	    .show();}
			else{
				testTTS=true;
				mTts.setLanguage(Locale.FRENCH);
				
				mTts.speak(Rep,TextToSpeech.QUEUE_FLUSH, null); // Il dicte sa phrase
			    Rep="";}} // Au cas o� Rep reste le m�me � la prochaine d�claration du TTS

	public void onDestroy(){ // Quitte le TTS quand l'application se termine
	    if (mTts != null){
	        mTts.stop();
	        mTts.shutdown();}
	    super.onDestroy();}
	
	public void onResume(){
		getConfig();
		super.onResume();}
	
    public boolean onCreateOptionsMenu(Menu menu) { // Il dit juste que y'a telle ou telle chose dans le menu
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);}   
	
	public boolean onOptionsItemSelected(MenuItem item){ // Il dit que si on clique sur tel objet, on effectue telle action
		if(item.getItemId() == R.id.Btnconfiguration){
			startActivityForResult(new Intent(this, Configuration.class), OPTION);}
		if(item.getItemId() == R.id.updateCom){
			Commandes_actu();}
		return super.onOptionsItemSelected(item);}

    void getConfig(){ // Importe les param�tres
    	SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
    	
    	String ip_adress;
    	if(Traitement.Verif_Reseau(getApplicationContext())){
    		ip_adress=preferences.getString("IPadress", "");}// Importe l'adresse du RPi
    	else{
    		ip_adress=preferences.getString("IPadress_ext", "");}// Importe l'adresse du RPi
    	
    	if(ip_adress != ""){
    		IPadress.setText(ip_adress);}
    	
    	AI = preferences.getBoolean("AI", true);
    		
    	Box_TTS=preferences.getBoolean("tts_pref", true); // Importe l'�tat de la box (autorise ou non le TTS)
    	if(Box_TTS==false){
    		tts_pref_false.setText("Attention ! Votre TTS est d�sactiv�.");}
    	else{
    		tts_pref_false.setText("");}
    	
    	bienvenue=preferences.getBoolean("bienvenue", true);
    	
    	update=preferences.getBoolean("update", false);
    	
    	Token=preferences.getString("token", "");
    	
    	Nom=preferences.getString("name", ""); // Importe l'identit� de la personne
		Pr�nom=preferences.getString("surname", "");
		Sexe=preferences.getString("sexe", "");
		Pseudo=preferences.getString("nickname", "");
    		
    	ShakeService=new Intent(Yana.this, ShakeService.class); // D�marre le service en fonction de l'�tat de la box
    	boolean Box_shake=preferences.getBoolean("shake", true);
    	if((Box_shake==true) && servstate==false){startService(ShakeService);}
    	else if((Box_shake==false) && servstate==true){stopService(ShakeService);}
    	else { // R�actualise les variables au cas o� on passe d'une reco en continu � une reco par Shake
    		stopService(ShakeService);
    		startService(ShakeService);}
    	
    	Traitement.Voice_Sens = Double.parseDouble(preferences.getString("Voice_sens", "3.0"))* Math.pow(10.0,-2.0); // Importe la sensibilit� de la comparaison des chaines de caract�res
    	if (Traitement.Voice_Sens>=1){
    		Toast t = Toast.makeText(getApplicationContext(),
    				"Attention ! La sensibilit� d'analyse de la voix est trop forte. Votre programme choisira la commande la plus proche de votre ordre. Pour mettre une sensibilit�, votre valeur dans les options doit �tre inf�rieure � 10. ",
    				Toast.LENGTH_SHORT);
    	        	t.show();}
    	
    	float Shake_sens=Float.parseFloat(preferences.getString("shake_sens", "3.0f")); // Importe la sensibilit� du Shake
		ShakeDetector.getConfig(Shake_sens);
		Log.d("End of Config","");}
        
    void Initialisation(){ // Initialise le processus
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "fr-FR");
			
		try {
			startActivityForResult(intent, RESULT_SPEECH);} // Lance l'acquisition vocale
			
		catch (ActivityNotFoundException a) {
			Toast t = Toast.makeText(getApplicationContext(),
					"Oh bah zut alors ! Ton Android n'a pas install� le STT ou ne le supporte pas. Regarde les options (langue et saisie).",
					Toast.LENGTH_SHORT);
			t.show();}
        }  

    void conversation(String Texte, String Envoi){ // Ici on inscrit la conversation entre l'utilisateur et le RPi
    	
    	final View Conversation_layout =  findViewById(R.id.conversation);
    	
        TextView valueTV = new TextView(this); // Cr�� le TextView pour afficher le message
        valueTV.setText(Texte);
        valueTV.setId(n);

        ImageView fleche = new ImageView(this); // Importe la petite fl�che de droite ou de gauche
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params_fleche = new RelativeLayout.LayoutParams(20, 20);
        	
        if(Envoi=="envoi"){
        	fleche.setImageResource(R.drawable.envoi);
        	params_fleche.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        	params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);
	        	
        	params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        	params.addRule(RelativeLayout.BELOW, (n-1));
	        	
        	valueTV.setBackgroundColor(getResources().getColor(R.color.envoi));}
        
        else{
        	fleche.setImageResource(R.drawable.reponse);
        	params_fleche.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        	params_fleche.addRule(RelativeLayout.ALIGN_BOTTOM, n);

        	params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        	params.addRule(RelativeLayout.BELOW, (n-1));
	        	
        	valueTV.setBackgroundColor(getResources().getColor(R.color.recu));}
        
        n=n+1;

        valueTV.setPadding(10, 10, 10, 10);
        params.setMargins(20, 0, 20, 20);
        params_fleche.setMargins(0, 0, 0, 20);
        
        valueTV.setLayoutParams(params);
        fleche.setLayoutParams(params_fleche);
        ((ViewGroup) Conversation_layout).addView(valueTV);
        ((ViewGroup) Conversation_layout).addView(fleche);
        
        ((ScrollView) findViewById(R.id.conversation_scroll)).post(new Runnable(){
            public void run(){((ScrollView) findViewById(R.id.conversation_scroll)).fullScroll(View.FOCUS_DOWN);}}); // Pour ancrer en bas � chaque nouvel ordre
    	}
    
	void Commandes_Layout(){ // Ici, on va inscrire les commandes sur le panel
    	
    	ListView Commandes_List =(ListView) findViewById(R.id.commandes_layout);
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, R.drawable.command_list, Traitement.Commandes);
		Commandes_List.setAdapter(modeAdapter);
	        
		Commandes_List.setOnItemClickListener(new OnItemClickListener() {
    	    public void onItemClick(AdapterView<?> arg0, View view, int arg2,long itemID) {
    	    	int ID=(int)itemID;
    	    	Pr�traitement(Traitement.Commandes.get(ID), Traitement.Liens.get(ID)); }
			});}

    void Commandes_actu(){ // Ici on va actualiser la liste des commandes
    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
 
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

		
    	if(!Commande_actu && Token.compareTo("")!=0){
    		if(activeNetwork!=null){
		    	if(Traitement.pick_JSON(IPadress.getText().toString(), Token)){ // Commence le protocole de reception et les enregistre dans une ArrayList
		    		Toast toast= Toast.makeText(getApplicationContext(), 
		    				"Update fait !", 4000);  
		    				toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
		    				toast.show();}
		    	
		    	else{
		    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
		    		Traitement.Commandes.get(1), 4000);  
					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
					toast.show();}}
		    else{
		    	Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
			    	"Vous n'avez pas de connexion internet !", 4000);  
					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
					toast.show();}
    	}
    	
    	else if (Token.compareTo("")==0 && !AI){ 
    		
    		Traitement.Commandes.clear();
    		Traitement.Liens.clear();
    		Traitement.Confidences.clear();
    		
    		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
    		"Vous n'avez pas entr� le Token. L'application ne peut pas communiquer avec votre Raspberry Pi.", 4000);  
    		toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    		toast.show();
    		
    		Traitement.Commandes.add(0, "YANA, cache-toi.");
			Traitement.Liens.add(0, "");
			Traitement.Confidences.add(0, "0.7");
			
    		Traitement.Commandes.add("Vous n'avez pas entr� le Token. L'application ne peut pas communiquer avec votre Raspberry Pi.");
    		Traitement.Liens.add("");
    		Traitement.Confidences.add("");}
    	
    	else{
    		Commande_actu=true;
    		Traitement.Commandes.clear();
    		Traitement.Liens.clear();
    		Traitement.Confidences.clear();
    		
    		Traitement.Commandes.add(0, "YANA, cache-toi.");
			Traitement.Liens.add(0, "");
			Traitement.Confidences.add(0, "0.7");
			
    		Traitement.Commandes.add("Vous n'avez pas encore actualis� vos commandes.");
    		Traitement.Liens.add("");
    		Traitement.Confidences.add("");}
    	
    	Commandes_Layout();}
    
    void Pr�traitement(final String Ordre, final String URL){ // Ici, on va analyser la r�ponse si elle est traitable localement. Sinon, on l'envoie au RPi
    	conversation(Ordre, "envoi");
    	
    	myHandler.postDelayed(new Runnable(){

		@Override
		public void run() {
			Pr�traitement2(Ordre,URL);
		}}, 250);
    }  
    
    void Pr�traitement2 (String Ordre, String URL){ // Deuxi�me partie du Pr�traitement (MyHandler l'oblige pour afficher l'ordre avant le traitement)
    	Rep="";
    	Log.d("Reco_invalide",""+Traitement.reco_invalide);
    	  
    	if(Ordre.compareTo(Traitement.Commandes.get(0))==0){ // V�rification du "Yana, cache-toi"
    		SharedPreferences.Editor geted = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		geted.putBoolean("shake", false);
    		geted.commit();
    		if(servstate==true){
    			stopService(ShakeService);
    			Rep="Le ShakeService est maintenant d�sactiv�.";}
    		else{Rep="Votre service est d�j� d�sactiv�.";}
    	}
    	
    	else if(Ordre.compareTo(Recrep)==0 && !Traitement.reco_invalide){Rep="Aucun ordre ne semble �tre identifi� au votre.";} // Si Ordre=Recrep alors c'est que la reconnaissance par pertinence a �chou�
    	else if(Traitement.reco_invalide){Rep="Vous n'avez pas activ� la reconaissance adapt�e pour cette commande.";}
    	else{
    		ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
     
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        	
        	if(activeNetwork!=null){ // V�rifie le r�seau
        		Log.d("Ordre",""+Ordre);
        			Rep = Traitement.HTTP_Contact("http://"+IPadress.getText().toString()+"?"+URL+"&token="+Token);} // Envoie au RPi et enregistre sa r�ponse
        	else{
        		Toast toast= Toast.makeText(getApplicationContext(), // En cas d'�chec, il pr�vient l'utilisateur
    			    	"Vous n'avez pas de connexion internet !", 4000);  
    					toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 80);
    					toast.show();}
        }
		if(Rep.compareTo("")!=0){
			conversation(Rep, "reponse");
			
			if(Box_TTS==true && Rep.length()<300){
				mTts = new TextToSpeech(this, this);} // Lance la synth�se vocale si les options l'autorisent et si la r�ponse n'est pas trop longue
    }}
    
    public String Random_String(){ // Choisit une chaine de caract�res au hasard
		ArrayList<String> list = new ArrayList<String>();
		list.add("Bonjour !");
		
		if(!AI){
			if(Pr�nom.compareTo("")!=0){
				list.add("Salut "+Pr�nom+" !");}
			
			if(Nom.compareTo("")!=0){
				list.add("Sinc�res salutations, ma�tre "+Nom+".");}
			
			if(Sexe.compareTo("")!=0){
				list.add("Bonjour "+Sexe+" "+Nom+". Heureux de vous revoir.");}
			
			if(Pseudo.compareTo("")!=0){
				list.add("Coucou mon petit "+Pseudo+". Heureux de te revoir !");}}
		
		int randomInt = random.nextInt(list.size());
        String Retour = list.get(randomInt).toString();
		
		return Retour;}

}