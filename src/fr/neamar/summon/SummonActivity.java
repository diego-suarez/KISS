package fr.neamar.summon;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import fr.neamar.summon.dataprovider.AppProvider;
import fr.neamar.summon.dataprovider.ContactProvider;
import fr.neamar.summon.dataprovider.Provider;
import fr.neamar.summon.dataprovider.SearchProvider;
import fr.neamar.summon.record.Record;
import fr.neamar.summon.record.RecordAdapter;
import fr.neamar.summon.record.RecordComparator;

public class SummonActivity extends Activity {

	private static final int MENU_SETTINGS = Menu.FIRST;

	private final int MAX_RECORDS = 15;

	/**
	 * Adapter to display records
	 */
	private RecordAdapter adapter;

	/**
	 * Pointer to current activity
	 */
	private SummonActivity summonActivity = this;

	/**
	 * List all knowns providers
	 */
	private ArrayList<Provider> providers = new ArrayList<Provider>();

	/**
	 * List view displaying records
	 */
	private ListView listView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Initialize UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getWindow().setBackgroundDrawable(
				getResources().getDrawable(R.drawable.background_holo_dark));

		listView = (ListView) findViewById(R.id.resultListView);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View arg1,
					int position, long id) {
				adapter.onClick(position);
			}
		});

		// Initialize providers
		providers.add(new AppProvider(getApplicationContext()));
		providers.add(new ContactProvider(getApplicationContext()));
		providers.add(new SearchProvider(getApplicationContext()));

		// Create adapter for records
		adapter = new RecordAdapter(getApplicationContext(), R.layout.item_app,
				new ArrayList<Record>());
		listView.setAdapter(adapter);

		// Listen to changes
		EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				summonActivity.updateRecords(s.toString());
			}
		});
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				return true;
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				return true;
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		}

		return super.dispatchKeyEvent(event);
	}

	/**
	 * Empty text field on resume
	 */
	protected void onResume() {
		EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
		searchEditText.setText("");

		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setIntent(
						new Intent(android.provider.Settings.ACTION_SETTINGS));

		return true;
	}

	/**
	 * This function gets called on changes. It will ask all the providers for
	 * datas
	 * 
	 * @param s
	 */
	public void updateRecords(String query) {
		adapter.clear();

		// Save currentQuery
		SharedPreferences prefs = getSharedPreferences("history",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("currentQuery", query);
		ed.commit();

		if (query.isEmpty()) {
			// Searching for nothing...
			populateHistory();
			return;
		}

		// Ask all providers for datas
		ArrayList<Record> allRecords = new ArrayList<Record>();

		// Have we ever made the same query and selected something ?
		String lastIdForQuery = prefs.getString("query://" + query, "(none)");

		for (int i = 0; i < providers.size(); i++) {
			ArrayList<Record> records = providers.get(i).getRecords(query);
			for (int j = 0; j < records.size(); j++) {
				// Give a boost if item was previously selected for this query
				if (records.get(j).holder.id.equals(lastIdForQuery))
					records.get(j).relevance += 50;
				allRecords.add(records.get(j));
			}
		}

		// Sort records according to relevance
		Collections.sort(allRecords, new RecordComparator());

		for (int i = 0; i < Math.min(MAX_RECORDS, allRecords.size()); i++) {
			adapter.add(allRecords.get(i));
		}

		// Reset scrolling to top
		listView.setSelectionAfterHeaderView();
	}

	private void populateHistory() {

		// Read history
		ArrayList<String> ids = new ArrayList<String>();
		SharedPreferences prefs = getSharedPreferences("history",
				Context.MODE_PRIVATE);

		for (int k = 0; ids.size() < MAX_RECORDS; k++) {
			String id = prefs.getString(Integer.toString(k), "(none)");

			// Not enough history yet
			if (id.equals("(none)"))
				break;

			// No duplicates, only keep recent
			if (!ids.contains(id))
				ids.add(id);
		}

		// Find associated items
		for (int i = 0; i < ids.size(); i++) {
			for (int j = 0; j < providers.size(); j++) {
				Record record = providers.get(j).findById(ids.get(i));
				if (record != null) {
					adapter.add(record);
					break;
				}
			}
		}
	}
}