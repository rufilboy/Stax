package com.hover.stax.channels;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.hover.sdk.sims.MultiSimTelephonyWorker;
import com.hover.stax.database.DatabaseRepo;
import com.hover.stax.sims.Sim;
import com.hover.stax.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ChannelViewModel extends AndroidViewModel {
	public final static String TAG = "ChannelViewModel";

	private DatabaseRepo repo;

	private MutableLiveData<List<Sim>> sims;
	LiveData<List<String>> simHniList = new MutableLiveData<>();
	LiveData<List<String>> simCountryList = new MutableLiveData<>();

	private LiveData<List<Channel>> allChannels;
	private MediatorLiveData<List<Integer>> selected;
	private MediatorLiveData<List<Channel>> simChannels;
	private MediatorLiveData<List<Channel>> countryChannels;

	public ChannelViewModel(Application application) {
		super(application);
		repo = new DatabaseRepo(application);
		loadChannels();
		loadSims();
		simHniList = Transformations.map(sims, this::getHnis);
		simCountryList = Transformations.map(sims, this::getSimCountries);

		simChannels = new MediatorLiveData<>();
		simChannels.addSource(allChannels, this::onChannelsUpdateHnis);
		simChannels.addSource(simHniList, this::onSimUpdate);

		countryChannels = new MediatorLiveData<>();
		countryChannels.addSource(allChannels, this::onChannelsUpdateCountries);
		countryChannels.addSource(simCountryList, this::onCountryUpdate);
	}

	LiveData<List<Channel>> getChannels() {
		return allChannels;
	}

	private void loadChannels() {
		if (allChannels == null) {
			allChannels = new MutableLiveData<>();
		}
		allChannels = repo.getAll();
		initSelected();
	}

	private void initSelected() {
		if (selected == null) {
			selected = new MediatorLiveData<>();
		}
		selected.addSource(allChannels, this::loadSelected);
	}

	private void loadSelected(List<Channel> channels) {
		List<Integer> ls = new ArrayList<>();
		for (Channel channel : channels) {
			if (channel.selected) ls.add(channel.id);
		}
		if (selected.getValue() != null) {
			ls.addAll(selected.getValue());
		}
		selected.setValue(ls);
	}

	LiveData<List<Integer>> getSelected() {
		return selected;
	}

	void loadSims() {
		if (sims == null) {
			sims = new MutableLiveData<>();
		}
		new Thread(() -> sims.postValue(repo.getSims())).start();
		LocalBroadcastManager.getInstance(getApplication())
			.registerReceiver(simReceiver, new IntentFilter(MultiSimTelephonyWorker.action(getApplication())));
	}

	private final BroadcastReceiver simReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			new Thread(() -> sims.postValue(repo.getSims())).start();
		}
	};

	private List<String> getHnis(List<Sim> sims) {
		if (sims == null) return null;
		List<String> hniList = new ArrayList<>();
		for (Sim sim : sims) {
			if (!hniList.contains(sim.hni)) {
				FirebaseMessaging.getInstance().subscribeToTopic("sim-" + sim.hni);
				hniList.add(sim.hni);
			}
		}
		return hniList;
	}

	private List<String> getSimCountries(List<Sim> sims) {
		if (sims == null) return null;
		List<String> countries = new ArrayList<>();
		for (Sim sim : sims) {
			if (!countries.contains(sim.country_iso.toUpperCase())) {
				countries.add(sim.country_iso.toUpperCase());
				FirebaseMessaging.getInstance().subscribeToTopic(sim.country_iso);
			}
		}
		return countries;
	}

	private void onChannelsUpdateHnis(List<Channel> channels) {
		updateSimChannels(channels, simHniList.getValue());
	}

	private void onSimUpdate(List<String> hniList) {
		updateSimChannels(allChannels.getValue(), hniList);
	}

	public void updateSimChannels(List<Channel> channels, List<String> hniList) {
		if (channels == null || hniList == null) return;
		List<Channel> simChanneList = new ArrayList<>();
		for (int i = 0; i < channels.size(); i++) {
			String[] hniArr = channels.get(i).hniList.split(",");
			for (String s : hniArr) {
				if (hniList.contains(Utils.stripHniString(s))) {
					if (!simChanneList.contains(channels.get(i)))
						simChanneList.add(channels.get(i));
				}
			}
		}
		simChannels.setValue(simChanneList);
	}

	public LiveData<List<Channel>> getSimChannels() {
		return simChannels;
	}

	private void onChannelsUpdateCountries(List<Channel> channels) {
		updateCountryChannels(channels, simCountryList.getValue());
	}

	private void onCountryUpdate(List<String> countryList) {
		updateCountryChannels(allChannels.getValue(), countryList);
	}

	public void updateCountryChannels(List<Channel> channels, List<String> countryList) {
		if (channels == null || countryList == null) return;
		List<Channel> countryChannelList = new ArrayList<>();
		for (int i = 0; i < channels.size(); i++) {
			for (String country : countryList) {
				if (country.equals(channels.get(i).countryAlpha2.toUpperCase()))
					countryChannelList.add(channels.get(i));
			}
		}
		countryChannels.setValue(countryChannelList);
	}

	public LiveData<List<Channel>> getCountryChannels() {
		return countryChannels;
	}

	void setSelected(int id) {
		List<Integer> list = selected.getValue() != null ? selected.getValue() : new ArrayList<>();
		if (list.contains(id))
			list.remove((Integer) id);
		else
			list.add(id);
		selected.setValue(list);
	}

	void saveSelected() {
		List<Channel> saveChannels = allChannels.getValue() != null ? allChannels.getValue() : new ArrayList<>();
		for (Channel channel : saveChannels) {
			if (selected.getValue().contains(channel.id)) {
				channel.selected = true;
				repo.update(channel);
				FirebaseMessaging.getInstance().subscribeToTopic("channel-" + channel.id);
			}
		}
	}

	@Override
	protected void onCleared() {
		try { LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(simReceiver);
		} catch (Exception ignored) { }
		super.onCleared();
	}
}
