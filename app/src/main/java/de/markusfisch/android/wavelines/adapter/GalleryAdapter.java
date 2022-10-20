package de.markusfisch.android.wavelines.adapter;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Database;
import de.markusfisch.android.wavelines.widget.GalleryItemView;

public class GalleryAdapter
		extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
	public interface ItemClickListener {
		void onItemClick(View view, long id, int position);

		boolean onItemLongClick(View view, long id, int position);
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		private final GalleryItemView themeView;

		private ViewHolder(GalleryItemView themeView) {
			super(themeView);
			this.themeView = themeView;
		}
	}

	private ItemClickListener itemClickListener;
	private Cursor cursor;

	public void swapCursor(Cursor cursor) {
		if (this.cursor == cursor) {
			return;
		}
		if (cursor != null) {
			this.cursor = cursor;
			notifyDataSetChanged();
		} else {
			notifyItemRangeRemoved(0, getItemCount());
			this.cursor = null;
		}
	}

	public void setClickListener(ItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	@Override
	public GalleryAdapter.ViewHolder onCreateViewHolder(
			ViewGroup parent,
			int viewType) {
		return new ViewHolder(new GalleryItemView(parent.getContext()));
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, int position) {
		if (!cursor.moveToPosition(position)) {
			return;
		}
		holder.themeView.setTheme(Database.themeFromCursor(cursor));
		final long id = Database.getLong(cursor, Database.THEMES_ID);
		holder.themeView.setSelected(WaveLinesApp.preferences.getTheme() == id);
		holder.themeView.setOnClickListener(v -> {
			if (itemClickListener != null) {
				itemClickListener.onItemClick(v, id,
						holder.getAdapterPosition());
			}
		});
		holder.themeView.setOnLongClickListener(v -> {
			if (itemClickListener != null) {
				return itemClickListener.onItemLongClick(v, id,
						holder.getAdapterPosition());
			}
			return false;
		});
	}

	@Override
	public int getItemCount() {
		return cursor != null ? cursor.getCount() : 0;
	}
}
