package com.florianmski.tracktoid.ui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import com.florianmski.tracktoid.R;
import com.florianmski.tracktoid.TraktoidConstants;
import com.florianmski.tracktoid.adapters.lists.ListRecommendationAdapter;
import com.florianmski.tracktoid.adapters.lists.ListRecommendationAdapter.DismissListener;
import com.florianmski.tracktoid.trakt.tasks.TraktTask;
import com.florianmski.tracktoid.trakt.tasks.get.GenresTask;
import com.florianmski.tracktoid.trakt.tasks.get.GenresTask.GenresListener;
import com.florianmski.tracktoid.trakt.tasks.get.TraktItemsTask;
import com.florianmski.tracktoid.trakt.tasks.get.TraktItemsTask.TraktItemsListener;
import com.florianmski.tracktoid.trakt.tasks.post.PostTask;
import com.florianmski.tracktoid.trakt.tasks.post.PostTask.PostListener;
import com.florianmski.tracktoid.ui.activities.phone.ShowActivity;
import com.florianmski.traktoid.TraktoidInterface;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.entities.DismissResponse;
import com.jakewharton.trakt.entities.Genre;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.services.RecommendationsService.MoviesBuilder;
import com.jakewharton.trakt.services.RecommendationsService.ShowsBuilder;

public abstract class RecommendationFragment<T extends TraktoidInterface<T>> extends TraktFragment
{	
	protected final static int START_YEAR = 1919;
	protected final static int END_YEAR = 2019;
	
	protected ListView lvRecommendations;
	protected Spinner spGenre, spStartYear, spEndYear;
	protected ImageView ivSend;

	protected ListRecommendationAdapter<T> adapter;

	protected List<Genre> genres;
	protected List<T> items;

	public RecommendationFragment() {}

	public RecommendationFragment(FragmentListener listener) 
	{
		super(listener);
	}
	
	public abstract TraktApiBuilder<DismissResponse> getDismissBuilder(String id);
	public abstract TraktApiBuilder<List<T>> getRecommendationBuilder(Genre genre);
	public abstract TraktApiBuilder<List<Genre>> getGenreBuilder();

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		
		if(savedInstanceState == null)
		{
			getStatusView().show().text("Retrieving genres,\nPlease wait...");

			new GenresTask(tm, this, new GenresListener() 
			{
				@Override
				public void onGenres(final List<Genre> genres) 
				{				
					RecommendationFragment.this.genres = genres;
					createGetRecommendationsTask().fire();
				}
			}, getGenreBuilder()).fire();
		}
		else
			setAdapter();

		lvRecommendations.setOnItemClickListener(new OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) 
			{
				Intent intent = new Intent(getActivity(), ShowActivity.class);
				intent.putExtra(TraktoidConstants.BUNDLE_RESULTS, (ArrayList<T>) adapter.getItems());
				intent.putExtra(TraktoidConstants.BUNDLE_POSITION, position-1);
				startActivity(intent);
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		View v = inflater.inflate(R.layout.fragment_recommendation, null);

		lvRecommendations = (ListView)v.findViewById(R.id.listViewRecommendation);
		spGenre = (Spinner) v.findViewById(R.id.spinnerGenre);
		spStartYear = (Spinner) v.findViewById(R.id.spinnerStartYear);
		spEndYear = (Spinner) v.findViewById(R.id.spinnerEndYear);
		ivSend = (ImageView) v.findViewById(R.id.imageViewSend);

		return v;
	}

	protected void setAdapter()
	{
		if(adapter == null)
		{
			lvRecommendations.setAdapter(adapter = new ListRecommendationAdapter<T>(items, getActivity()));
			
			String[] itemsGenres = new String[genres.size()+1];
			itemsGenres[0] = "All Genres";

			for(int i = 1; i < itemsGenres.length; i++)
				itemsGenres[i] = genres.get(i-1).name;

			ArrayAdapter<String> spinnerAdapterGenres = new ArrayAdapter<String>(getActivity(), R.layout.sherlock_spinner_item, itemsGenres);
			spinnerAdapterGenres.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
			spGenre.setAdapter(spinnerAdapterGenres);
			
			int size = END_YEAR - START_YEAR + 1;
			String[] itemsStartYear = new String[size];
			String[] itemsEndYear = new String[size];
			for(int i = 0; i < size; i++)
			{
				itemsStartYear[i] = String.valueOf(START_YEAR+i);
				itemsEndYear[i] = String.valueOf(END_YEAR-i);
			}
			ArrayAdapter<String> spinnerAdapterStartYear = new ArrayAdapter<String>(getActivity(), R.layout.sherlock_spinner_item, itemsStartYear);
			spinnerAdapterStartYear.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
			spStartYear.setAdapter(spinnerAdapterStartYear);
			ArrayAdapter<String> spinnerAdapterEndYear = new ArrayAdapter<String>(getActivity(), R.layout.sherlock_spinner_item, itemsEndYear);
			spinnerAdapterEndYear.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
			spEndYear.setAdapter(spinnerAdapterEndYear);
			
			ivSend.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					adapter.clear();
					createGetRecommendationsTask().fire();
				}
			});
		}
		else
		{
			adapter.updateItems(items);
			if(lvRecommendations.getAdapter() == null)
				lvRecommendations.setAdapter(adapter);
		}

		if(adapter.getCount() == 0)
			getStatusView().hide().text("No recommendations, strange...");
		else
			getStatusView().hide().text(null);

		adapter.setOnDismissListener(new DismissListener() 
		{
			@Override
			public void onDismiss(String id) 
			{
				new PostTask(tm, RecommendationFragment.this, getDismissBuilder(id), new PostListener() 
				{
					@Override
					public void onComplete(Response r, boolean success) 
					{
						adapter.clear();
						createGetRecommendationsTask().fire();
					}
				}).fire();
			}
		});
	}

	protected TraktTask createGetRecommendationsTask()
	{
		int index = spGenre == null ? -1 : spGenre.getSelectedItemPosition();
		Genre genre = index <= 0 || index > genres.size() ? null : genres.get(index-1);
		getStatusView().show().text("Retrieving recommendations" + ((genre == null) ? "" : " in \"" + genre.name + "\"") + ",\nPlease wait...");	

		return commonTask = new TraktItemsTask<T>(tm, this, new TraktItemsListener<T>() 
		{
			@Override
			public void onTraktItems(List<T> traktItems) 
			{
				RecommendationFragment.this.items = traktItems;
				setAdapter();
			}
		}, getRecommendationBuilder(genre), false);
	}

	@Override
	public void onRestoreState(Bundle savedInstanceState) {}

	@Override
	public void onSaveState(Bundle toSave) {}
	
	public static class RecommendationMoviesFragment extends RecommendationFragment<Movie>
	{	
		public static RecommendationMoviesFragment newInstance(Bundle args)
		{
			RecommendationMoviesFragment f = new RecommendationMoviesFragment();
			f.setArguments(args);
			return f;
		}

		public RecommendationMoviesFragment() {}

		public RecommendationMoviesFragment(FragmentListener listener) 
		{
			super(listener);
		}

		@Override
		public TraktApiBuilder<DismissResponse> getDismissBuilder(String id)
		{
			return tm.recommendationsService().dismissMovie(id);
		}

		@Override
		public TraktApiBuilder<List<Movie>> getRecommendationBuilder(Genre genre) 
		{
			MoviesBuilder builder = tm.recommendationsService().movies();

			if(genre != null)
				builder.genre(genre);
			
			if(spStartYear != null && spEndYear != null)
				builder.startYear(spStartYear.getSelectedItemPosition() + START_YEAR).endYear(END_YEAR - spEndYear.getSelectedItemPosition());
			
			return builder;
		}

		@Override
		public TraktApiBuilder<List<Genre>> getGenreBuilder() 
		{
			return tm.genreService().movies();
		}
	}
	
	public static class RecommendationShowsFragment extends RecommendationFragment<TvShow>
	{	
		public static RecommendationShowsFragment newInstance(Bundle args)
		{
			RecommendationShowsFragment f = new RecommendationShowsFragment();
			f.setArguments(args);
			return f;
		}

		public RecommendationShowsFragment() {}

		public RecommendationShowsFragment(FragmentListener listener) 
		{
			super(listener);
		}

		@Override
		public TraktApiBuilder<DismissResponse> getDismissBuilder(String id)
		{
			return tm.recommendationsService().dismissShow(Integer.valueOf(id));
		}

		@Override
		public TraktApiBuilder<List<TvShow>> getRecommendationBuilder(Genre genre) 
		{
			ShowsBuilder builder = tm.recommendationsService().shows();

			if(genre != null)
				builder.genre(genre);
			
			if(spStartYear != null && spEndYear != null)
				builder.startYear(spStartYear.getSelectedItemPosition() + START_YEAR).endYear(END_YEAR - spEndYear.getSelectedItemPosition());
			
			return builder;
		}

		@Override
		public TraktApiBuilder<List<Genre>> getGenreBuilder() 
		{
			return tm.genreService().shows();
		}
	}
}
