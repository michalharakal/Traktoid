package com.florianmski.tracktoid.ui.fragments.pagers.items;

import java.util.ArrayList;
import java.util.List;

import net.londatiga.android.QuickAction;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.florianmski.tracktoid.TraktoidConstants;
import com.florianmski.tracktoid.adapters.GridPosterAdapter;
import com.florianmski.tracktoid.db.tasks.DBAdapter;
import com.florianmski.tracktoid.db.tasks.DBMoviesTask;
import com.florianmski.tracktoid.trakt.TraktManager;
import com.florianmski.tracktoid.trakt.tasks.RemoveMovieTask;
import com.florianmski.tracktoid.trakt.tasks.get.TraktItemsTask;
import com.florianmski.tracktoid.trakt.tasks.get.TraktItemsTask.TraktItemsListener;
import com.florianmski.tracktoid.trakt.tasks.get.UpdateMoviesTask;
import com.florianmski.tracktoid.ui.activities.phone.MovieActivity;
import com.jakewharton.trakt.entities.Movie;

public class MoviesLibraryFragment extends PagerItemLibraryFragment<Movie>
{
	public static MoviesLibraryFragment newInstance(Bundle args)
	{
		MoviesLibraryFragment f = new MoviesLibraryFragment();
		f.setArguments(args);
		return f;
	}
	
	public MoviesLibraryFragment() {}

	@Override
	public void checkUpdateTask() 
	{
		//TODO
	}

	@Override
	public GridPosterAdapter<Movie> setupAdapter() 
	{
		return new GridPosterAdapter<Movie>(getActivity(), new ArrayList<Movie>(), refreshGridView());
	}

	@Override
	public Intent onGridItemClick(AdapterView<?> arg0, View v, int position, long arg3) 
	{
		Intent i = new Intent(getActivity(), MovieActivity.class);
		ArrayList<Movie> movies = new ArrayList<Movie>();
		movies.add(adapter.getItem(position));
		i.putExtra(TraktoidConstants.BUNDLE_RESULTS, movies);
		return i;
	}
	
	@Override
	public void displayContent() 
	{
		if(getDBWrapper().isThereMovies())
		{
			new DBMoviesTask(getActivity(), new DBAdapter() 
			{
				@Override
				public void onDBMovies(List<Movie> movies)
				{
					adapter.updateItems(movies);
					getStatusView().hide().text(null);
				}
			}).fire();
		}
	}

	@Override
	public void onRefreshQAClick(QuickAction source, int pos, int actionId) 
	{
		ArrayList<Movie> moviesSelected = new ArrayList<Movie>();
		moviesSelected.add(adapter.getItem(posterClickedPosition));
		tm.addToQueue(new UpdateMoviesTask(tm, MoviesLibraryFragment.this, moviesSelected));
	}

	@Override
	public void onDeleteQAClick(QuickAction source, int pos, int actionId) 
	{
		tm.addToQueue(new RemoveMovieTask(tm, MoviesLibraryFragment.this, adapter.getItem(posterClickedPosition)));
	}

	@Override
	public void onRateQAClick(QuickAction source, int pos, int actionId) 
	{
		//TODO
	}

	@Override
	public void onRefreshClick() 
	{
		tm.addToQueue(new TraktItemsTask<Movie>(tm, this, new TraktItemsListener<Movie>() 
		{
			@Override
			public void onTraktItems(List<Movie> movies) 
			{
				createMoviesDialog(movies);					
			}
		}, tm.userService().libraryMoviesAll(TraktManager.getUsername()), true));
	}
	
	public void createMoviesDialog(final List<Movie> movies)
	{
		final ArrayList<Movie> selectedMovies = new ArrayList<Movie>();

		String[] items = new String[movies.size()];

		for(int i = 0; i < movies.size(); i++)
			items[i] = movies.get(i).title;

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Which movies(s) do you want to refresh ?");
		builder.setMultiChoiceItems(items, null, new OnMultiChoiceClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) 
			{
				if(isChecked)
					selectedMovies.add(movies.get(which));
				else
					selectedMovies.remove(movies.get(which));
			}
		});

		builder.setPositiveButton("Go!", new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				if(selectedMovies.size() > 0)
					tm.addToQueue(new UpdateMoviesTask(tm, MoviesLibraryFragment.this, selectedMovies));
				else
					Toast.makeText(getActivity(), "Nothing selected...", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNeutralButton("All", new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				tm.addToQueue(new UpdateMoviesTask(tm, MoviesLibraryFragment.this, movies));
			}
		});

		builder.setNegativeButton("Cancel", new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) {}
		});

		AlertDialog alert = builder.create();

		//avoid trying to show dialog if activity no longer exist
		if(!getActivity().isFinishing())
			alert.show();
	}

	@Override
	public void onMovieUpdated(Movie movie)
	{		
		if(adapter != null)
			adapter.updateItem(movie);
	}

	@Override
	public void onMovieRemoved(Movie movie)
	{
		if(adapter != null)
			adapter.remove(movie);
	}

}
