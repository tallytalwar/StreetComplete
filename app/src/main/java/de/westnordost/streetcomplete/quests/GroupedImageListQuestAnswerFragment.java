package de.westnordost.streetcomplete.quests;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import de.westnordost.streetcomplete.Injector;
import de.westnordost.streetcomplete.R;
import de.westnordost.streetcomplete.view.GroupedImageSelectAdapter;
import de.westnordost.streetcomplete.view.Item;
import de.westnordost.streetcomplete.view.dialogs.AlertDialogBuilder;

/**
 * Abstract class for quests with a grouped list of images and one to select.
 *
 * Saving and restoring state is not implemented
 */
public abstract class GroupedImageListQuestAnswerFragment extends AbstractQuestFormAnswerFragment
{
	public static final String OSM_VALUE = "osm_value";

	protected GroupedImageSelectAdapter imageSelector;
	private Button showMoreButton;
	private RecyclerView valueList;

	private List<Item> allItems;
	private List<Item> topItems;

	@Inject LastPickedValuesStore favs;

	@Override public void onCreate(Bundle inState)
	{
		super.onCreate(inState);
		Injector.instance.getApplicationComponent().inject(this);
		allItems = Collections.unmodifiableList(Arrays.asList(getAllItems()));
		topItems = Collections.unmodifiableList(Arrays.asList(getTopItems()));
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
									   Bundle savedInstanceState)
	{
		View view = super.onCreateView(inflater, container, savedInstanceState);

		View contentView = setContentView(R.layout.quest_generic_list);

		valueList = contentView.findViewById(R.id.listSelect);
		GridLayoutManager lm = new GridLayoutManager(getActivity(), getItemsPerRow());
		valueList.setLayoutManager(lm);
		valueList.setNestedScrollingEnabled(false);

		showMoreButton = contentView.findViewById(R.id.buttonShowMore);
		showMoreButton.setOnClickListener(v ->
		{
			imageSelector.setItems(allItems);
			showMoreButton.setVisibility(View.GONE);
		});

		TextView selectHint = contentView.findViewById(R.id.selectHint);
		selectHint.setText(R.string.quest_select_hint_most_specific);

		imageSelector = new GroupedImageSelectAdapter(lm);

		return view;
	}

	@Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		imageSelector.setItems(getInitialItems());
		valueList.setAdapter(imageSelector);
	}

	@Override protected void onClickOk()
	{
		Item item = getSelectedItem();
		if(item == null)
		{
			Toast.makeText(getActivity(), R.string.no_changes, Toast.LENGTH_SHORT).show();
			return;
		}

		if(item.isGroup())
		{
			if(!item.hasValue())
			{
				new AlertDialogBuilder(getContext())
					.setMessage(R.string.quest_generic_item_invalid_value)
					.setPositiveButton(R.string.ok, null)
					.show();
			}
			else
			{
				new AlertDialogBuilder(getContext())
					.setMessage(R.string.quest_generic_item_confirmation)
					.setNegativeButton(R.string.quest_generic_confirmation_no, null)
					.setPositiveButton(R.string.quest_generic_confirmation_yes,
						(dialog, which) -> applyAnswerAndSave(item.value))
					.show();
			}
		}
		else
		{
			applyAnswerAndSave(item.value);
		}
	}

	private void applyAnswerAndSave(String value)
	{
		favs.addLastPicked(getClass().getSimpleName(), value);
		applyAnswer(value);
	}

	protected void applyAnswer(String value)
	{
		Bundle answer = new Bundle();
		answer.putString(OSM_VALUE, value);
		applyFormAnswer(answer);
	}

	@Override public boolean hasChanges() { return getSelectedItem() != null; }

	private Item getSelectedItem() { return imageSelector.getSelectedItem(); }

	protected int getItemsPerRow() { return 3; }

	protected abstract Item[] getTopItems();
	protected abstract Item[] getAllItems();

	private List<Item> getInitialItems()
	{
		LinkedList<Item> items = new LinkedList<>(topItems);
		favs.moveLastPickedToFront(getClass().getSimpleName(), items, allItems);
		return items;
	}
}
